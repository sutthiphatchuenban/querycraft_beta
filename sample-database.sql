-- Sample Database for QueryCraft Testing
-- ฐานข้อมูลตัวอย่างสำหรับทดสอบโปรแกรม QueryCraft
-- 
-- วิธีใช้:
-- 1. เปิด phpMyAdmin ใน XAMPP
-- 2. คลิกที่ "Import"
-- 3. เลือกไฟล์นี้แล้วกด "Go"

-- สร้างฐานข้อมูล
CREATE DATABASE IF NOT EXISTS querycraft_test 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE querycraft_test;

-- ============================================
-- ตาราง: departments (แผนก)
-- ============================================
CREATE TABLE departments (
    dept_id INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    budget DECIMAL(12, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO departments (dept_name, location, budget) VALUES
('ฝ่ายขาย', 'กรุงเทพฯ', 1500000.00),
('ฝ่ายการตลาด', 'กรุงเทพฯ', 2500000.00),
('ฝ่ายไอที', 'เชียงใหม่', 3000000.00),
('ฝ่ายบุคคล', 'กรุงเทพฯ', 800000.00),
('ฝ่ายการเงิน', 'กรุงเทพฯ', 1200000.00);

-- ============================================
-- ตาราง: employees (พนักงาน)
-- ============================================
CREATE TABLE employees (
    emp_id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    hire_date DATE,
    salary DECIMAL(10, 2),
    dept_id INT,
    position VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO employees (first_name, last_name, email, phone, hire_date, salary, dept_id, position, is_active) VALUES
('สมชาย', 'ใจดี', 'somchai@company.com', '081-234-5678', '2020-01-15', 45000.00, 1, 'ผู้จัดการฝ่ายขาย', TRUE),
('สมหญิง', 'รักเรียน', 'somying@company.com', '082-345-6789', '2020-03-20', 35000.00, 1, 'พนักงานขาย', TRUE),
('ประเสริฐ', 'มากมี', 'prasert@company.com', '083-456-7890', '2019-06-10', 65000.00, 2, 'ผู้จัดการการตลาด', TRUE),
('มานี', 'สวยงาม', 'manee@company.com', '084-567-8901', '2021-02-01', 28000.00, 2, 'นักการตลาด', TRUE),
('สมศักดิ์', 'เข้มแข็ง', 'somsak@company.com', '085-678-9012', '2018-09-25', 75000.00, 3, 'CTO', TRUE),
('บุญมี', 'มั่งคั่ง', 'boonmee@company.com', '086-789-0123', '2022-05-15', 42000.00, 3, 'นักพัฒนา', TRUE),
('สุดา', 'รัตน์', 'suda@company.com', '087-890-1234', '2021-08-20', 38000.00, 4, 'เจ้าหน้าที่ HR', TRUE),
('ประทีป', 'แสงสว่าง', 'prateep@company.com', '088-901-2345', '2020-11-10', 55000.00, 5, 'หัวหน้าการเงิน', TRUE),
('อังคณา', 'วิสัย', 'angkan@company.com', '089-012-3456', '2023-01-05', 25000.00, 1, 'พนักงานขาย', TRUE),
('วิชัย', 'เก่งกาจ', 'wichai@company.com', '090-123-4567', '2019-12-01', 40000.00, 3, 'นักพัฒนา', FALSE);

-- ============================================
-- ตาราง: products (สินค้า)
-- ============================================
CREATE TABLE products (
    product_id INT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(20) UNIQUE NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO products (product_code, product_name, description, category, price, stock_quantity, is_available) VALUES
('NB-001', 'Notebook Dell Inspiron 15', 'โน้ตบุ๊คสำหรับงานทั่วไป', 'คอมพิวเตอร์', 18900.00, 50, TRUE),
('NB-002', 'Notebook HP Pavilion 14', 'โน้ตบุ๊คบางเบา', 'คอมพิวเตอร์', 22900.00, 30, TRUE),
('MB-001', 'Mouse Wireless Logitech', 'เมาส์ไร้สาย', 'อุปกรณ์เสริม', 590.00, 200, TRUE),
('KB-001', 'Keyboard Mechanical Keychron', 'คีย์บอร์ด机械', 'อุปกรณ์เสริม', 3490.00, 100, TRUE),
('MN-001', 'Monitor Samsung 24 inch', 'จอมอนิเตอร์ 24 นิ้ว', 'อุปกรณ์เสริม', 5490.00, 40, TRUE),
('SP-001', 'Smartphone Samsung Galaxy S24', 'สมาร์ทโฟนรุ่นใหม่', 'มือถือ', 32900.00, 25, TRUE),
('SP-002', 'iPhone 15 Pro', 'ไอโฟนรุ่นโปร', 'มือถือ', 41900.00, 20, TRUE),
('HD-001', 'External HDD 1TB', 'ฮาร์ดดิสก์พกพา 1TB', 'อุปกรณ์จัดเก็บ', 1890.00, 80, TRUE),
('SW-001', 'Microsoft Office 365', 'โปรแกรมออฟฟิศ', 'ซอฟต์แวร์', 3290.00, 999, TRUE),
('PR-001', 'Printer Canon PIXMA', 'เครื่องพิมพ์อิงค์เจ็ท', 'เครื่องพิมพ์', 3990.00, 15, TRUE),
('RT-001', 'Router WiFi 6 TP-Link', 'เราเตอร์ WiFi 6', 'เน็ตเวิร์ก', 2490.00, 60, TRUE),
('CAM-001', 'Webcam Logitech C920', 'เว็บแคม HD', 'อุปกรณ์เสริม', 3290.00, 0, FALSE);

-- ============================================
-- ตาราง: customers (ลูกค้า)
-- ============================================
CREATE TABLE customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(20) UNIQUE NOT NULL,
    company_name VARCHAR(200),
    contact_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(50),
    postal_code VARCHAR(10),
    customer_type ENUM('บุคคลธรรมดา', 'นิติบุคคล') DEFAULT 'บุคคลธรรมดา',
    credit_limit DECIMAL(12, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO customers (customer_code, company_name, contact_name, email, phone, address, city, postal_code, customer_type, credit_limit, is_active) VALUES
('CUS-001', 'บริษัท เอบีซี จำกัด', 'คุณสมศักดิ์', 'contact@abc.co.th', '02-123-4567', '123 ถนนสุขุมวิท', 'กรุงเทพฯ', '10110', 'นิติบุคคล', 100000.00, TRUE),
('CUS-002', NULL, 'คุณวิชัย', 'wichai@email.com', '081-111-2222', '456 ถนนเพชรบุรี', 'กรุงเทพฯ', '10400', 'บุคคลธรรมดา', 0, TRUE),
('CUS-003', 'บริษัท ไทยเทค จำกัด', 'คุณมานี', 'manee@thaitech.com', '02-987-6543', '789 ถนนรัชดาภิเษก', 'กรุงเทพฯ', '10310', 'นิติบุคคล', 500000.00, TRUE),
('CUS-004', NULL, 'คุณประเสริฐ', 'prasert@gmail.com', '082-222-3333', '12 ถนนนิมมานเหมินทร์', 'เชียงใหม่', '50200', 'บุคคลธรรมดา', 0, TRUE),
('CUS-005', 'บริษัท ซีดีอี จำกัด', 'คุณสุดา', 'suda@cde.co.th', '02-555-6666', '99 อาคารสิรินาถ', 'กรุงเทพฯ', '10500', 'นิติบุคคล', 200000.00, TRUE),
('CUS-006', 'ร้านค้าดีดี', 'คุณบุญมี', 'boonmee@ddshop.com', '053-111-222', '45 ถนนวัวลาย', 'เชียงใหม่', '50100', 'นิติบุคคล', 50000.00, TRUE),
('CUS-007', NULL, 'คุณสมหญิง', 'somying@hotmail.com', '083-333-4444', '78 ซอยลาดพร้าว 15', 'กรุงเทพฯ', '10310', 'บุคคลธรรมดา', 0, FALSE),
('CUS-008', 'บริษัท อีเอฟจี จำกัด', 'คุณประทีป', 'prateep@efg.co.th', '02-777-8888', '150 ถนนพระราม 9', 'กรุงเทพฯ', '10320', 'นิติบุคคล', 300000.00, TRUE);

-- ============================================
-- ตาราง: orders (คำสั่งซื้อ)
-- ============================================
CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id INT NOT NULL,
    emp_id INT NOT NULL,
    order_date DATE NOT NULL,
    required_date DATE,
    shipped_date DATE,
    status ENUM('Pending', 'Processing', 'Shipped', 'Delivered', 'Cancelled') DEFAULT 'Pending',
    shipping_address TEXT,
    subtotal DECIMAL(12, 2) DEFAULT 0,
    tax_amount DECIMAL(10, 2) DEFAULT 0,
    discount_amount DECIMAL(10, 2) DEFAULT 0,
    total_amount DECIMAL(12, 2) DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO orders (order_number, customer_id, emp_id, order_date, required_date, shipped_date, status, shipping_address, subtotal, tax_amount, discount_amount, total_amount, notes) VALUES
('ORD-2024-001', 1, 2, '2024-01-15', '2024-01-20', '2024-01-18', 'Delivered', '123 ถนนสุขุมวิท กรุงเทพฯ 10110', 38900.00, 2723.00, 0, 41623.00, 'ส่งเร็วกว่ากำหนด'),
('ORD-2024-002', 3, 1, '2024-01-20', '2024-01-25', '2024-01-24', 'Delivered', '789 ถนนรัชดาภิเษก กรุงเทพฯ 10310', 65790.00, 4605.30, 1000.00, 69395.30, NULL),
('ORD-2024-003', 2, 2, '2024-02-05', '2024-02-10', NULL, 'Processing', '456 ถนนเพชรบุรี กรุงเทพฯ 10400', 2480.00, 173.60, 0, 2653.60, 'รอสินค้าเข้า'),
('ORD-2024-004', 5, 9, '2024-02-14', '2024-02-20', NULL, 'Pending', '99 อาคารสิรินาถ กรุงเทพฯ 10500', 82980.00, 5808.60, 2000.00, 86788.60, 'ลูกค้าขอส่วนลดพิเศษ'),
('ORD-2024-005', 4, 2, '2024-03-01', '2024-03-05', '2024-03-04', 'Delivered', '12 ถนนนิมมานเหมินทร์ เชียงใหม่ 50200', 5480.00, 383.60, 0, 5863.60, NULL),
('ORD-2024-006', 1, 1, '2024-03-10', '2024-03-15', NULL, 'Processing', '123 ถนนสุขุมวิท กรุงเทพฯ 10110', 113800.00, 7966.00, 5000.00, 116766.00, 'สั่งซื้อจำนวนมาก'),
('ORD-2024-007', 6, 9, '2024-03-18', '2024-03-22', NULL, 'Pending', '45 ถนนวัวลาย เชียงใหม่ 50100', 14900.00, 1043.00, 0, 15943.00, NULL);

-- ============================================
-- ตาราง: order_items (รายการสินค้าในคำสั่งซื้อ)
-- ============================================
CREATE TABLE order_items (
    item_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    line_total DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO order_items (order_id, product_id, quantity, unit_price, discount_percent, line_total) VALUES
(1, 1, 1, 18900.00, 0, 18900.00),
(1, 3, 2, 590.00, 0, 1180.00),
(1, 4, 1, 3490.00, 10, 3141.00),
(1, 5, 1, 5490.00, 0, 5490.00),
(1, 8, 1, 1890.00, 0, 1890.00),
(1, 9, 1, 3290.00, 0, 3290.00),
(1, 11, 1, 2490.00, 0, 2490.00),
(2, 2, 1, 22900.00, 0, 22900.00),
(2, 6, 1, 32900.00, 0, 32900.00),
(2, 7, 1, 41900.00, 5, 39805.00),
(2, 10, 1, 3990.00, 0, 3990.00),
(2, 9, 1, 3290.00, 0, 3290.00),
(3, 4, 1, 3490.00, 0, 3490.00),
(3, 3, 1, 590.00, 0, 590.00),
(3, 8, 1, 1890.00, 0, 1890.00),
(4, 6, 2, 32900.00, 5, 62510.00),
(4, 7, 1, 41900.00, 0, 41900.00),
(4, 9, 1, 3290.00, 0, 3290.00),
(5, 5, 1, 5490.00, 0, 5490.00),
(5, 3, 2, 590.00, 0, 1180.00),
(6, 2, 2, 22900.00, 10, 41220.00),
(6, 7, 2, 41900.00, 0, 83800.00),
(6, 10, 1, 3990.00, 0, 3990.00),
(7, 1, 1, 18900.00, 0, 18900.00),
(7, 4, 1, 3490.00, 0, 3490.00);

-- ============================================
-- อัพเดทยอดรวมในตาราง orders
-- ============================================
UPDATE orders o 
SET subtotal = (SELECT SUM(line_total) FROM order_items WHERE order_id = o.order_id),
    tax_amount = (SELECT SUM(line_total) FROM order_items WHERE order_id = o.order_id) * 0.07,
    total_amount = (SELECT SUM(line_total) FROM order_items WHERE order_id = o.order_id) * 1.07 - discount_amount;

-- ============================================
-- สร้าง View สำหรับรายงาน
-- ============================================
CREATE VIEW v_order_summary AS
SELECT 
    o.order_id,
    o.order_number,
    o.order_date,
    c.customer_code,
    COALESCE(c.company_name, c.contact_name) AS customer_name,
    CONCAT(e.first_name, ' ', e.last_name) AS sales_person,
    o.status,
    o.total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
JOIN employees e ON o.emp_id = e.emp_id;

CREATE VIEW v_top_products AS
SELECT 
    p.product_code,
    p.product_name,
    p.category,
    SUM(oi.quantity) AS total_sold,
    SUM(oi.line_total) AS total_revenue
FROM products p
LEFT JOIN order_items oi ON p.product_id = oi.product_id
GROUP BY p.product_id
ORDER BY total_sold DESC;

-- ============================================
-- สร้าง Index เพื่อเพิ่มประสิทธิภาพ
-- ============================================
CREATE INDEX idx_employees_dept ON employees(dept_id);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_employee ON orders(emp_id);
CREATE INDEX idx_orders_date ON orders(order_date);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- ============================================
-- เสร็จสิ้น
-- ============================================
SELECT 'Database querycraft_test created successfully!' AS status;
SELECT CONCAT('Departments: ', COUNT(*)) FROM departments;
SELECT CONCAT('Employees: ', COUNT(*)) FROM employees;
SELECT CONCAT('Products: ', COUNT(*)) FROM products;
SELECT CONCAT('Customers: ', COUNT(*)) FROM customers;
SELECT CONCAT('Orders: ', COUNT(*)) FROM orders;
