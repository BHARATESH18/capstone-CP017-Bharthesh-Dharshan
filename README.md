# Problem Title: TrackPoint: Order Lifecycle and Delivery Tracking System

**Problem ID:** CP-017

## Team Members

1. **Name:** Bharatesh | **USN:** 1AT24CS042
2. **Name:** Dharshan | **USN:** 1AT24CS048

## Problem Description

TrackPoint is an Order Lifecycle and Delivery Tracking System designed to simplify order management and help customers track their orders. The system allows users to place orders, update delivery statuses, view complete order history, and identify delayed orders.

It reduces customer support workload by providing clear and updated information about order delivery progress.

## How to Run the Program

**Prerequisites:**
- JDK 
- intelij IDE.
- Git installed.

**Steps:**

1. Clone or download this repository.
2. Open the project in inteliji ide
3. Compile the program:

   ```bash
   run the main
   ```

4. Execute the program:

   **For Linux/macOS:**
   ```bash
   ./main
   ```

   **For Windows:**
   ```bash
   main.exe
   ```

## Features Implemented

1. **Place an Order:** Enter customer details, ordered items, and delivery pincode. Generate an order ID automatically.

2. **Update Order Status:** Move orders through the following delivery stages:
   - Placed
   - Packed
   - Shipped
   - Out for Delivery
   - Delivered

   Only valid next status transitions are allowed.

3. **Track Order:** Search for an order using its order ID and display its complete status history.

4. **Delayed Order Detection:** Identify and list orders that have remained in the same status for more than 2 days.

5. **Customer Order History:** Display the complete order history of a particular customer.

## Sample Run

**Input:**

```text
========== TRACKPOINT ==========
ORDER LIFECYCLE AND DELIVERY TRACKING SYSTEM

1. Place New Order
2. Update Order Status
3. Track Order
4. View Delayed Orders
5. Customer Order History
6. Exit

Enter your choice: 1

Enter Customer Name: Rahul
Enter Item Name: Laptop
Enter Delivery Pincode: 560001

Order Placed Successfully!

Generated Order ID: 1001
Current Status: Placed
```

**Output:**

```text
========== TRACK ORDER ==========

Enter Order ID: 1001

Order ID: 1001
Customer Name: Rahul
Item: Laptop
Delivery Pincode: 560001

Order Status History:
1. Placed
2. Packed
3. Shipped
4. Out for Delivery
5. Delivered

Current Status: Placed

=================================
```

## Technologies Used

- Programming Language: C
- Compiler: GCC
- IDE: Visual Studio Code
- Version Control: Git and GitHub

## Project Outcome

TrackPoint provides a simple and efficient console-based solution for managing the complete order lifecycle. It improves order tracking, maintains delivery status history, identifies delayed orders, and helps customers access their order information easily.

---
