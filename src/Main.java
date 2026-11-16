import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** CP-017 TrackPoint: Order Lifecycle and Delivery Tracking System. */
public class Main {
    private static final Path DATA_FILE = Paths.get("data", "orders.csv");
    private static final Scanner SC = new Scanner(System.in);
    private static final Map<String, Order> ORDERS = new LinkedHashMap<>();
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    enum Status { PLACED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }
    static class Event {
        final Status status; final LocalDateTime at; final String note;
        Event(Status status, LocalDateTime at, String note) { this.status=status; this.at=at; this.note=note; }
    }
    static class Order {
        String id, customer, items, pincode;
        LocalDateTime createdAt;
        Status status;
        LocalDateTime statusSince;
        List<Event> history = new ArrayList<>();
        Order(String id, String customer, String items, String pincode, LocalDateTime createdAt) {
            this.id=id; this.customer=customer; this.items=items; this.pincode=pincode;
            this.createdAt=createdAt; this.status=Status.PLACED; this.statusSince=createdAt;
            history.add(new Event(Status.PLACED, createdAt, "Order placed"));
        }
    }

    public static void main(String[] args) {
        loadOrders();
        System.out.println("==============================================");
        System.out.println("       TRACKPOINT - ORDER DELIVERY TRACKER");
        System.out.println("==============================================");
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ", 0, 8);
            switch (choice) {
                case 1 -> placeOrder();
                case 2 -> updateStatus();
                case 3 -> trackOrder();
                case 4 -> showDelayedOrders();
                case 5 -> customerHistory();
                case 6 -> estimateDelivery();
                case 7 -> cancelOrder();
                case 8 -> listAllOrders();
                case 0 -> { saveOrders(); running=false; System.out.println("Data saved. Thank you for using TrackPoint."); }
                default -> System.out.println("Invalid menu option.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n--------------- MAIN MENU ----------------");
        System.out.println("1. Place a new order");
        System.out.println("2. Update order status");
        System.out.println("3. Track an order (full history)");
        System.out.println("4. Show orders delayed more than 2 days");
        System.out.println("5. Show customer's complete order history");
        System.out.println("6. Estimate delivery date (stretch)");
        System.out.println("7. Cancel an order (stretch)");
        System.out.println("8. List all orders");
        System.out.println("0. Save and exit");
        System.out.println("-------------------------------------------");
    }

    private static void placeOrder() {
        System.out.println("\n--- PLACE ORDER ---");
        String customer = readNonBlank("Customer name: ");
        String items = readNonBlank("Items (separate multiple items with semicolons): ");
        String pin;
        while (true) {
            pin = readNonBlank("6-digit delivery pincode: ");
            if (pin.matches("[1-9][0-9]{5}")) break;
            System.out.println("Invalid pincode. Enter a valid 6-digit Indian PIN code.");
        }
        String id = nextOrderId();
        Order order = new Order(id, customer, items, pin, LocalDateTime.now());
        ORDERS.put(id, order);
        saveOrders();
        System.out.println("Order placed successfully. Your Order ID is: " + id);
    }

    private static void updateStatus() {
        System.out.println("\n--- UPDATE ORDER STATUS ---");
        Order o = findOrder(readNonBlank("Enter order ID: "));
        if (o == null) return;
        if (o.status == Status.DELIVERED || o.status == Status.CANCELLED) {
            System.out.println("This order is " + pretty(o.status) + " and cannot be updated."); return;
        }
        Status next = nextStatus(o.status);
        System.out.println("Current status: " + pretty(o.status));
        System.out.println("Next valid status: " + pretty(next));
        String confirm = readNonBlank("Type YES to confirm this status change: ");
        if (!confirm.equalsIgnoreCase("YES")) { System.out.println("Update cancelled; no changes made."); return; }
        transition(o, next, "Status updated by operator");
        saveOrders();
        System.out.println("Status updated successfully.");
    }

    private static void transition(Order o, Status next, String note) {
        o.status = next; o.statusSince = LocalDateTime.now();
        o.history.add(new Event(next, o.statusSince, note));
    }

    private static Status nextStatus(Status current) {
        return switch (current) {
            case PLACED -> Status.PACKED;
            case PACKED -> Status.SHIPPED;
            case SHIPPED -> Status.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> Status.DELIVERED;
            default -> current;
        };
    }

    private static void trackOrder() {
        System.out.println("\n--- TRACK ORDER ---");
        Order o = findOrder(readNonBlank("Enter order ID: "));
        if (o == null) return;
        printOrder(o);
        System.out.println("Status history:");
        for (Event e : o.history) System.out.printf("  %-18s %s | %s%n", pretty(e.status), e.at.format(DISPLAY), e.note);
    }

    private static void showDelayedOrders() {
        System.out.println("\n--- ORDERS IN CURRENT STATUS FOR MORE THAN 2 DAYS ---");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(2);
        int count=0;
        for (Order o : ORDERS.values()) {
            if (o.status != Status.DELIVERED && o.status != Status.CANCELLED && o.statusSince.isBefore(cutoff)) {
                System.out.printf("%s | %s | %s | In %s for %.1f days%n", o.id, o.customer, o.pincode,
                        pretty(o.status), Duration.between(o.statusSince, LocalDateTime.now()).toMinutes()/1440.0);
                count++;
            }
        }
        if (count==0) System.out.println("No orders have remained in a status for more than 2 days.");
    }

    private static void customerHistory() {
        System.out.println("\n--- CUSTOMER ORDER HISTORY ---");
        String name = readNonBlank("Enter customer name (case-insensitive): ");
        int count=0;
        for (Order o : ORDERS.values()) if (o.customer.equalsIgnoreCase(name)) { printOrder(o); count++; }
        if (count==0) System.out.println("No orders found for customer: " + name);
        else System.out.println("Total orders for " + name + ": " + count);
    }

    private static void estimateDelivery() {
        System.out.println("\n--- ESTIMATED DELIVERY (STRETCH FEATURE) ---");
        Order o = findOrder(readNonBlank("Enter order ID: "));
        if (o == null) return;
        int days = zoneDays(o.pincode);
        LocalDate estimate = o.createdAt.toLocalDate().plusDays(days);
        System.out.println("Pincode zone: " + zoneName(o.pincode));
        System.out.println("Estimated delivery date: " + estimate);
        System.out.println("Estimate is illustrative and is not a live courier commitment.");
    }

    // Simple demo rule: first PIN digit 1-3 = local, 4-6 = regional, 7-9 = extended.
    private static int zoneDays(String pin) {
        int first = pin.charAt(0)-'0';
        if (first <= 3) return 2;
        if (first <= 6) return 4;
        return 6;
    }
    private static String zoneName(String pin) {
        int first=pin.charAt(0)-'0';
        return first<=3 ? "Local (2 days)" : first<=6 ? "Regional (4 days)" : "Extended (6 days)";
    }

    private static void cancelOrder() {
        System.out.println("\n--- CANCEL ORDER (STRETCH FEATURE) ---");
        Order o = findOrder(readNonBlank("Enter order ID: "));
        if (o == null) return;
        if (o.status != Status.PLACED && o.status != Status.PACKED) {
            System.out.println("Cancellation rejected. Orders can only be cancelled before they are shipped."); return;
        }
        String confirm=readNonBlank("Type YES to cancel order " + o.id + ": ");
        if (!confirm.equalsIgnoreCase("YES")) { System.out.println("Cancellation aborted."); return; }
        transition(o, Status.CANCELLED, "Cancelled before shipment");
        saveOrders();
        System.out.println("Order cancelled successfully.");
    }

    private static void listAllOrders() {
        System.out.println("\n--- ALL ORDERS ---");
        if (ORDERS.isEmpty()) { System.out.println("No orders available."); return; }
        for (Order o : ORDERS.values()) printOrder(o);
    }

    private static void printOrder(Order o) {
        System.out.printf("Order ID: %s | Customer: %s | Items: %s | Pincode: %s | Status: %s | Since: %s%n",
                o.id, o.customer, o.items, o.pincode, pretty(o.status), o.statusSince.format(DISPLAY));
    }
    private static String pretty(Status s) { return s.name().replace('_',' '); }

    private static Order findOrder(String id) {
        Order o=ORDERS.get(id.trim().toUpperCase(Locale.ROOT));
        if (o==null) System.out.println("Order not found. Check the order ID and try again.");
        return o;
    }
    private static String nextOrderId() {
        int max=0;
        for (String id:ORDERS.keySet()) try { max=Math.max(max,Integer.parseInt(id.substring(3))); } catch(Exception ignored) {}
        return String.format("TP-%04d", max+1);
    }
    private static String readNonBlank(String prompt) {
        while(true) { System.out.print(prompt); String s=SC.nextLine().trim(); if(!s.isEmpty()) return s; System.out.println("Input cannot be empty."); }
    }
    private static int readInt(String prompt,int min,int max) {
        while(true) { System.out.print(prompt); String s=SC.nextLine().trim();
            try { int n=Integer.parseInt(s); if(n>=min && n<=max) return n; }
            catch(NumberFormatException ignored) {}
            System.out.println("Enter a whole number from " + min + " to " + max + ".");
        }
    }

    // Each user-entered text field is Base64-encoded, so commas/newlines do not corrupt the CSV.
    private static String enc(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
    private static void saveOrders() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            try(BufferedWriter w=Files.newBufferedWriter(DATA_FILE,StandardCharsets.UTF_8)) {
                w.write("id,customer,items,pincode,createdAt,status,statusSince,history\n");
                for(Order o:ORDERS.values()) {
                    List<String> ev=new ArrayList<>();
                    for(Event e:o.history) ev.add(e.status.name()+"~"+e.at+"~"+enc(e.note));
                    w.write(String.join(",", enc(o.id),enc(o.customer),enc(o.items),enc(o.pincode),o.createdAt.toString(),
                            o.status.name(),o.statusSince.toString(),enc(String.join("|",ev)))); w.newLine();
                }
            }
        } catch(IOException ex) { System.out.println("Warning: could not save orders: " + ex.getMessage()); }
    }
    private static void loadOrders() {
        if(!Files.exists(DATA_FILE)) return;
        try(BufferedReader r=Files.newBufferedReader(DATA_FILE,StandardCharsets.UTF_8)) {
            String line=r.readLine(); // header
            while((line=r.readLine())!=null) {
                try {
                    String[] p=line.split(",",-1); if(p.length!=8) continue;
                    Order o=new Order(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3]),LocalDateTime.parse(p[4]));
                    o.status=Status.valueOf(p[5]); o.statusSince=LocalDateTime.parse(p[6]); o.history.clear();
                    String hist=dec(p[7]);
                    if(!hist.isEmpty()) for(String item:hist.split("\\|")) {
                        String[] h=item.split("~",3);
                        o.history.add(new Event(Status.valueOf(h[0]),LocalDateTime.parse(h[1]),dec(h[2])));
                    }
                    ORDERS.put(o.id,o);
                } catch(Exception badRow) { System.out.println("Skipped one invalid data row in orders.csv."); }
            }
        } catch(IOException ex) { System.out.println("Warning: could not load saved orders: " + ex.getMessage()); }
    }
}
