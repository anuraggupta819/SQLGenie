-- Sample business dataset that SQLGenie's assistant answers natural-language
-- questions about. Lives in its own schema so it can be granted to a
-- dedicated read-only Postgres role, kept entirely separate from `app`.

CREATE SCHEMA IF NOT EXISTS target;

CREATE TABLE target.customers (
    id           SERIAL PRIMARY KEY,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    city         VARCHAR(100),
    country      VARCHAR(100),
    signup_date  DATE NOT NULL
);

CREATE TABLE target.products (
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    category       VARCHAR(100) NOT NULL,
    price          NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL
);

CREATE TABLE target.orders (
    id           SERIAL PRIMARY KEY,
    customer_id  INTEGER NOT NULL REFERENCES target.customers (id),
    order_date   DATE NOT NULL,
    status       VARCHAR(20) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL
);

CREATE TABLE target.order_items (
    id         SERIAL PRIMARY KEY,
    order_id   INTEGER NOT NULL REFERENCES target.orders (id),
    product_id INTEGER NOT NULL REFERENCES target.products (id),
    quantity   INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL
);

INSERT INTO target.customers (first_name, last_name, email, city, country, signup_date) VALUES
    ('Alice', 'Nguyen', 'alice.nguyen@example.com', 'Seattle', 'USA', '2024-01-15'),
    ('Bharat', 'Sharma', 'bharat.sharma@example.com', 'Mumbai', 'India', '2024-02-20'),
    ('Chloe', 'Martin', 'chloe.martin@example.com', 'Paris', 'France', '2024-03-05'),
    ('David', 'Kim', 'david.kim@example.com', 'Seoul', 'South Korea', '2024-03-18'),
    ('Elena', 'Rossi', 'elena.rossi@example.com', 'Milan', 'Italy', '2024-04-02'),
    ('Farid', 'Haddad', 'farid.haddad@example.com', 'Dubai', 'UAE', '2024-05-11'),
    ('Grace', 'Okafor', 'grace.okafor@example.com', 'Lagos', 'Nigeria', '2024-05-27'),
    ('Hiro', 'Tanaka', 'hiro.tanaka@example.com', 'Tokyo', 'Japan', '2024-06-09'),
    ('Isla', 'Campbell', 'isla.campbell@example.com', 'Toronto', 'Canada', '2024-07-14'),
    ('Javier', 'Torres', 'javier.torres@example.com', 'Madrid', 'Spain', '2024-08-01');

INSERT INTO target.products (name, category, price, stock_quantity) VALUES
    ('Wireless Mouse', 'Electronics', 24.99, 150),
    ('Mechanical Keyboard', 'Electronics', 89.99, 80),
    ('USB-C Hub', 'Electronics', 39.99, 200),
    ('Standing Desk', 'Furniture', 349.99, 25),
    ('Office Chair', 'Furniture', 199.99, 40),
    ('Desk Lamp', 'Furniture', 29.99, 100),
    ('Notebook Set', 'Stationery', 12.99, 300),
    ('Fountain Pen', 'Stationery', 18.99, 120),
    ('Water Bottle', 'Accessories', 15.99, 250),
    ('Backpack', 'Accessories', 59.99, 90);

INSERT INTO target.orders (customer_id, order_date, status, total_amount) VALUES
    (1, '2024-06-01', 'DELIVERED', 114.98),
    (1, '2024-08-12', 'DELIVERED', 39.99),
    (2, '2024-06-15', 'DELIVERED', 349.99),
    (3, '2024-07-02', 'SHIPPED', 229.98),
    (4, '2024-07-20', 'DELIVERED', 89.99),
    (5, '2024-08-05', 'CANCELLED', 199.99),
    (6, '2024-08-18', 'DELIVERED', 75.97),
    (7, '2024-09-01', 'PENDING', 59.99),
    (8, '2024-09-10', 'DELIVERED', 44.98),
    (9, '2024-09-22', 'SHIPPED', 349.99),
    (10, '2024-10-03', 'DELIVERED', 24.99),
    (2, '2024-10-15', 'DELIVERED', 89.99),
    (3, '2024-10-28', 'PENDING', 15.99);

INSERT INTO target.order_items (order_id, product_id, quantity, unit_price) VALUES
    (1, 1, 1, 24.99), (1, 3, 1, 39.99), (1, 6, 1, 29.99), (1, 7, 2, 9.99),
    (2, 3, 1, 39.99),
    (3, 4, 1, 349.99),
    (4, 5, 1, 199.99), (4, 6, 1, 29.99),
    (5, 2, 1, 89.99),
    (6, 5, 1, 199.99),
    (7, 9, 1, 15.99), (7, 10, 1, 59.99),
    (8, 10, 1, 59.99),
    (9, 1, 1, 24.99), (9, 9, 1, 15.99),
    (10, 4, 1, 349.99),
    (11, 1, 1, 24.99),
    (12, 2, 1, 89.99),
    (13, 9, 1, 15.99);
