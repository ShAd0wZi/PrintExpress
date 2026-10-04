-- ==========================================================
-- PrintXpress Supabase (PostgreSQL) Schema & Initial Seed
-- Migrated directly matching local DB schema / entities
-- Language / Stack: Java Android Client
-- ==========================================================

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT,
    password_hash TEXT NOT NULL,
    registration_date TIMESTAMPTZ DEFAULT NOW(),
    user_type TEXT NOT NULL DEFAULT 'CUSTOMER'
);

CREATE TABLE IF NOT EXISTS addresses (
    address_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    city TEXT NOT NULL,
    postal_code TEXT,
    phone TEXT,
    is_default BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS categories (
    category_id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    image_url TEXT
);

CREATE TABLE IF NOT EXISTS products (
    product_id BIGSERIAL PRIMARY KEY,
    category_id BIGINT REFERENCES categories(category_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    base_price NUMERIC(10,2) NOT NULL,
    image_url TEXT,
    available_sizes TEXT,
    available_materials TEXT,
    stock_quantity INT DEFAULT 100
);

CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(product_id) ON DELETE CASCADE,
    quantity INT NOT NULL,
    selected_size TEXT,
    selected_material TEXT,
    artwork_file_path TEXT,
    custom_text TEXT,
    unit_price NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    order_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    order_date TIMESTAMPTZ DEFAULT NOW(),
    total_amount NUMERIC(10,2) NOT NULL,
    delivery_method TEXT NOT NULL,
    delivery_address_id BIGINT REFERENCES addresses(address_id) ON DELETE SET NULL,
    payment_method TEXT NOT NULL,
    payment_status TEXT NOT NULL DEFAULT 'PENDING',
    order_status TEXT NOT NULL DEFAULT 'PENDING',
    scheduled_pickup_date TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(product_id) ON DELETE CASCADE,
    quantity INT NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    customisation_details TEXT,
    artwork_file_path TEXT,
    subtotal NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    is_read BOOLEAN DEFAULT FALSE,
    type TEXT
);

CREATE TABLE IF NOT EXISTS promotions (
    promotion_id BIGSERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    discount_percentage NUMERIC(5,2) NOT NULL,
    min_order_amount NUMERIC(10,2) NOT NULL,
    valid_until TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT TRUE
);

-- Seed: Users
INSERT INTO users (user_id, name, email, phone, password_hash, registration_date, user_type) VALUES (1, 'PrintXpress Press Admin', 'admin@printxpress.lk', '0112345000', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', to_timestamp(1785897730.213), 'ADMIN') ON CONFLICT (email) DO NOTHING;
INSERT INTO users (user_id, name, email, phone, password_hash, registration_date, user_type) VALUES (2, 'Kasun Perera (Apex Lanka)', 'demo@printxpress.lk', '0771234567', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', to_timestamp(1789872130.213), 'CUSTOMER') ON CONFLICT (email) DO NOTHING;

-- Seed: Addresses
INSERT INTO addresses (address_id, user_id, address_line1, address_line2, city, postal_code, phone, is_default) VALUES (1, 2, 'No. 42, Galle Road, Kollupitiya', 'Level 3, Apex Tower', 'Colombo 03', '00300', '0771234567', TRUE) ON CONFLICT (address_id) DO NOTHING;
INSERT INTO addresses (address_id, user_id, address_line1, address_line2, city, postal_code, phone, is_default) VALUES (2, 2, 'Block B, Orion City IT Park, 752 Baseline Road', 'Dematagoda', 'Colombo 09', '00900', '0112345678', FALSE) ON CONFLICT (address_id) DO NOTHING;

-- Seed: Categories
INSERT INTO categories (category_id, name, description, image_url) VALUES (1, 'Business Cards', '350gsm–400gsm matte, velvet soft-touch, spot UV & letterheads', 'ic_cat_business_cards') ON CONFLICT (category_id) DO NOTHING;
INSERT INTO categories (category_id, name, description, image_url) VALUES (2, 'Flyers', 'High-speed Heidelberg offset leaflets, tri-fold menus & posters', 'ic_cat_flyers') ON CONFLICT (category_id) DO NOTHING;
INSERT INTO categories (category_id, name, description, image_url) VALUES (3, 'Banners', 'Retractable pull-up stands, outdoor PVC flex & X-frame displays', 'ic_cat_banners') ON CONFLICT (category_id) DO NOTHING;
INSERT INTO categories (category_id, name, description, image_url) VALUES (4, 'T-Shirts', 'DTF screen-printed cotton tees, embroidered pique polos & hoodies', 'ic_cat_tshirts') ON CONFLICT (category_id) DO NOTHING;
INSERT INTO categories (category_id, name, description, image_url) VALUES (5, 'Mugs', 'Sublimated ceramic mugs, laser-engraved vacuum flasks & gift sets', 'ic_cat_mugs') ON CONFLICT (category_id) DO NOTHING;
INSERT INTO categories (category_id, name, description, image_url) VALUES (6, 'Stickers & Packaging', 'Waterproof die-cut vinyl stickers, roll labels & mailer boxes', 'ic_cat_stickers') ON CONFLICT (category_id) DO NOTHING;
INSERT INTO categories (category_id, name, description, image_url) VALUES (7, 'Books & Catalogs', 'Saddle-stitched booklets, annual reports, notebooks & calendars', 'ic_cat_booklets') ON CONFLICT (category_id) DO NOTHING;
INSERT INTO categories (category_id, name, description, image_url) VALUES (8, 'Events & ID Passes', 'CR80 PVC employee ID cards, sublimated satin lanyards & invitations', 'ic_cat_events') ON CONFLICT (category_id) DO NOTHING;

-- Seed: Products
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (1, 1, 'Premium Business Card', 'Double-sided full-color CMYK visiting cards printed on heavy 350gsm–400gsm European art board with optional velvet lamination.', 2500, 'premium_card', '90x54mm Standard (100 pcs), 85x55mm Euro (100 pcs), 65x65mm Square (100 pcs)', '350gsm Art Matte, 400gsm Velvet Soft-Touch, 350gsm High Gloss', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (2, 1, 'Spot UV & Gold Foil Visiting Cards', 'Executive luxury business cards featuring raised 3D Spot UV gloss or metallic gold/silver hot-foil stamping on 400gsm velvet card.', 4800, 'premium_card', '90x54mm Standard (100 pcs), 85x55mm Rounded Corners (100 pcs)', '400gsm Velvet Matte + Gold Foil, 400gsm Matte + Raised Spot UV, 350gsm Textured Linen', 12) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (3, 1, 'Corporate Letterheads (A4 Conqueror Stock)', 'Laser- and inkjet-safe official company letterheads with crisp offset watermark-grade reproduction for invoices and legal correspondence.', 3800, 'premium_card', 'A4 (210x297mm - 100 Sheets), A4 (210x297mm - 500 Sheets)', '100gsm Bank Bond, 120gsm Conqueror Wove, 120gsm Textured Laid', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (4, 1, 'Custom Branded DL & C5 Window Envelopes', 'Custom printed corporate envelopes with company return address and logo, featuring peel-and-seal adhesive strips.', 3200, 'premium_card', 'DL (110x220mm - 100 pcs), C5 (162x229mm - 100 pcs), C4 (229x324mm - 100 pcs)', '100gsm White Woodfree, 120gsm Kraft Brown, 120gsm Peel-and-Seal Window', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (5, 2, 'Standard Flyer A5', 'High-impact promotional handouts and event leaflets printed on Heidelberg offset presses with vibrant double-sided color.', 1500, 'flyer_a5', 'A5 Single-Sided (100 pcs), A5 Double-Sided (100 pcs), A4 Double-Sided (100 pcs)', '130gsm Gloss Art Paper, 150gsm Silk Matte, 170gsm Premium Art', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (6, 2, 'Tri-Fold Corporate Brochure & Takeaway Menu', 'Machine-creased 6-panel tri-fold or Z-fold brochures ideal for company profiles, hotel spa guides, and restaurant takeaway menus.', 4200, 'flyer_a5', 'A4 Tri-Fold to DL (50 pcs), A3 Tri-Fold to A4 (50 pcs)', '170gsm Gloss Art, 200gsm Matte Laminated, 250gsm Art Board', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (7, 2, 'DL Promotional Rack Cards & Gift Vouchers', 'Sturdy 300gsm–350gsm card stock vouchers, hotel door hangers, and counter display rack cards with optional perforation.', 2200, 'flyer_a5', 'DL 99x210mm (100 pcs), A6 Postcard (100 pcs)', '300gsm Art Card Glossy, 300gsm Matte Laminated, 350gsm Uncoated Writable', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (8, 2, 'A3 & A2 Retail Point-of-Sale Wall Posters', 'Large high-resolution indoor promotional posters for retail storefronts, tuition announcements, and concert promotions.', 2800, 'flyer_a5', 'A3 (297x420mm - 10 pcs), A2 (420x594mm - 10 pcs)', '170gsm High-Gloss Poster Paper, 200gsm Satin Photo Stock, 250gsm Art Card', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (9, 3, 'Roll-up Pull Banner', 'Retractable anodized aluminum cassette stand with padded carry bag and non-curl 1440dpi eco-solvent graphic print.', 7500, 'banner_rollup', 'Standard (2.5x6.5 ft / 85x200cm), Wide (3x6.5 ft / 100x200cm), Luxury Broad-Base (3x6.5 ft)', '300gsm Grey-Back Anti-Curl Film, 440gsm Matte Blockout Vinyl, Heavy-Duty Satin', 12) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (10, 3, 'Outdoor Heavy-Duty Weatherproof PVC Flex Banner', 'UV-resistant and waterproof outdoor commercial banner with reinforced welded hems and rust-proof brass eyelets every 2 feet.', 1800, 'banner_rollup', '4x2 ft (Eyelets Included), 6x3 ft (Eyelets Included), 8x4 ft Large Format, 10x5 ft Billboard', '440gsm Star Flex Glossy, 510gsm Black-Back Heavy Flex, Wind-Slit Mesh Vinyl', 12) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (11, 3, 'Portable X-Banner Display Stand with Print', 'Lightweight fiberglass and carbon-steel tension X-frame stand with quick-change corner grommet banner for exhibitions and lobbies.', 4500, 'banner_rollup', 'Standard (2x5 ft / 60x160cm), Large (2.5x6 ft / 80x180cm)', '340gsm Matte PVC Vinyl, 300gsm Tear-Resistant Polypropylene', 12) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (12, 3, 'Mounted Sunboard & Rigid PVC Foam Signage', 'Direct UV-flatbed or laminated vinyl mounted onto rigid 5mm foam board for indoor directional signage and exhibition booths.', 3400, 'banner_rollup', 'A3 (297x420mm), A2 (420x594mm), A1 (594x841mm)', '5mm High-Density Sunboard + Matte Vinyl, 3mm PVC Celuka Board, 5mm Gloss Mounted Foam', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (13, 4, 'Custom Cotton T-Shirt', 'Pre-shrunk 100% ring-spun combed cotton crew-neck tee with vibrant DTF full-color chest and back printing or plastisol screen print.', 3200, 'tshirt_cotton', 'S, M, L, XL, XXL', '190gsm 100% Combed Cotton, 220gsm Heavyweight Oversized Cotton, 160gsm Poly-Cotton Blend', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (14, 4, 'Embroidered Corporate Pique Polo Shirt', 'Smart-casual collared company uniform shirt featuring high-stitch-density computerized logo embroidery on left chest and sleeve.', 3950, 'tshirt_cotton', 'S, M, L, XL, XXL', '220gsm Honeycomb Pique Cotton, 200gsm Breathable Moisture-Wicking Polo, 240gsm Mercerized Cotton', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (15, 4, 'Dry-Fit Marathon & Corporate Cricket Jersey', 'Breathable moisture-wicking athletic jersey with edge-to-edge dye sublimation that never cracks, peels, or fades.', 2650, 'tshirt_cotton', 'S, M, L, XL, XXL', '150gsm Micro-Mesh Dry-Fit (Full Sublimation), 160gsm Anti-UV Athletic Poly', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (16, 4, 'Custom Fleece Pullover Company Hoodie', 'Warm double-lined hood pullover with kangaroo pocket, ribbed cuffs, and custom DTF or embroidered team branding.', 6800, 'tshirt_cotton', 'S, M, L, XL, XXL', '320gsm Brushed Cotton Fleece, 280gsm French Terry Loopback', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (17, 5, 'Custom Ceramic Mug', 'Dishwasher- and microwave-safe AAA-grade ceramic mug with full-wrap panoramic dye-sublimation photo or logo print.', 1200, 'mug_ceramic', '11 oz Standard (325ml), 15 oz Tall Executive (440ml)', 'AAA-Grade Glossy White Ceramic, Heat-Sensitive Matte Black Magic Mug, Inner-Color Rim Ceramic', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (18, 5, 'Laser-Engraved Double-Wall Thermal Vacuum Flask', 'Insulated 304 stainless-steel bottle keeping drinks cold for 24h or hot for 12h, permanently laser-etched with your company logo.', 3600, 'mug_ceramic', '500ml Insulated Bottle, 750ml Sports Vacuum Flask', '304 Double-Wall Matte Black Steel, Brushed Silver Stainless, Bamboo-Lid Eco Steel', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (19, 5, 'Executive Onboarding Corporate Welcome Gift Box', 'Curated new-hire and VIP client gift box containing a branded A5 PU notebook, metal rollerball pen, and insulated drinkware.', 8500, 'mug_ceramic', '3-Piece Starter Set (Notebook + Pen + Mug), 5-Piece VIP Set (+ Flask & 32GB Metal USB)', 'Matte Black Magnetic Presentation Box, Eco Kraft Rigid Gift Box', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (20, 6, 'Custom Die-Cut Waterproof Vinyl Stickers', 'Precision contour-cut stickers resistant to water, oil, and sunlight—ideal for laptops, merchandise packs, and bumper branding.', 1600, 'sticker_vinyl', '2x2 inch (50 pcs), 3x3 inch (50 pcs), 4x4 inch (50 pcs), A4 Kiss-Cut Sheet (10 Sheets)', 'White Waterproof PVC Vinyl + Matte Lamination, Clear Transparent Vinyl, Holographic Rainbow Vinyl', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (21, 6, 'Product Bottle, Cosmetic & Spice Jar Roll Labels', 'Commercial-grade self-adhesive roll or sheet labels engineered for food jars, beverage bottles, cosmetics, and export packaging.', 2900, 'sticker_vinyl', '50x30mm (250 Labels), 80x50mm (250 Labels), 100x70mm (250 Labels)', 'Waterproof BOPP Synthetic Gloss, Textured Estate Wine Paper, Eco Brown Kraft Adhesive', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (22, 6, 'Custom Printed E-Commerce Corrugated Mailer Boxes', 'Crush-resistant self-locking D2C shipping boxes printed inside and out with your brand colors and unboxing message.', 6500, 'sticker_vinyl', 'Small (6x4x2 in - 25 Boxes), Medium (9x6x3 in - 25 Boxes), Large (12x9x4 in - 25 Boxes)', '3-Ply E-Flute Brown Kraft Corrugated, White Top-Coated Corrugated, Matte Black Luxury Mailer', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (23, 7, 'Saddle-Stitched Product Catalog & Annual Report', 'Full-color multi-page booklets stapled on the spine with heavy laminated art-board covers and crisp gloss or silk inside pages.', 6200, 'booklet_catalog', 'A5 Portrait (16 Pages - 25 Copies), A4 Portrait (16 Pages - 25 Copies), A4 Portrait (32 Pages - 25 Copies)', '250gsm Art Card Cover + 130gsm Gloss Inner, 300gsm Matte Laminated Cover + 150gsm Silk Inner', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (24, 7, 'Hardcover & Wire-O Branded Company Notebooks', 'Custom corporate journals with 80gsm cream ivory ruled pages, ribbon bookmark, and custom printed or debossed cover.', 1850, 'booklet_catalog', 'A5 (148x210mm - 80 Ruled Leaves), B5 Executive (176x250mm - 100 Leaves)', 'PU Leather Debossed Hardcover, 350gsm Matte Laminated Wire-O Spiral, Eco Recycled Cork & Kraft', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (25, 7, '2027 Corporate Desk Tent Calendars', '13-leaf double-sided wiro-bound desk calendar featuring Sri Lankan Mercantile & Poya holidays and custom monthly brand imagery.', 4400, 'booklet_catalog', 'Landscape 8.5x6 in (13 Leaves - 10 pcs), Portrait 6x8.5 in (13 Leaves - 10 pcs)', '700gsm Rigid Linen Hardboard Stand + 200gsm Matte Art Leaves, Eco Kraft Stand + 230gsm Ivory Card', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (26, 8, 'CR80 PVC Employee ID Cards & Event Badges', 'High-definition thermal-transfer CR80 plastic ID cards with barcode/QR serialization, slot punch, and optional RFID proximity chip.', 950, 'event_lanyard', 'CR80 Credit Card Size (85.6x54mm - 5 Cards), A6 Oversized Event VIP Pass (5 Cards)', '760 Micron Glossy Thermal PVC, Matte Composite PVC with RFID/NFC Chip, Frosted Transparent PVC', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (27, 8, 'Full-Color Sublimated Satin Neck Lanyards', 'Silky-smooth double-sided polyester lanyards with edge-to-edge logo sublimation, metal trigger clip, and safety breakaway clasp.', 2400, 'event_lanyard', '15mm Width (10 Lanyards), 20mm Standard Width (10 Lanyards), 25mm Wide Event Strap (10 Lanyards)', 'Smooth Double-Sided Polyester Satin + Metal Lobster Hook, Safety Breakaway Buckle + Rigid ID Holder', 120) ON CONFLICT (product_id) DO NOTHING;
INSERT INTO products (product_id, category_id, name, description, base_price, image_url, available_sizes, available_materials, stock_quantity) VALUES (28, 8, 'Luxury Foil-Stamped Wedding & Gala Invitations', 'Bespoke invitations crafted on imported shimmer, velvet, or cotton card stock with matching wax-seal ready envelopes.', 5400, 'event_lanyard', '5x7 inch Flat Card + Envelope (50 Sets), 6x6 inch Laser-Cut Gatefold (50 Sets)', '350gsm Pearl Shimmer Ivory Board, 400gsm Emerald/Navy Velvet Card + Gold Foil, 320gsm Textured Cotton Rag', 12) ON CONFLICT (product_id) DO NOTHING;

-- Seed: Promotions
INSERT INTO promotions (promotion_id, code, title, description, discount_percentage, min_order_amount, valid_until, is_active) VALUES (1, 'WELCOME10', '10% Off First Order', 'Enjoy 10% discount on your first commercial print order.', 10, 1000, to_timestamp(1793673730.214), TRUE) ON CONFLICT (code) DO NOTHING;
INSERT INTO promotions (promotion_id, code, title, description, discount_percentage, min_order_amount, valid_until, is_active) VALUES (2, 'PXBIZ20', '20% OFF Bulk Business Cards', 'Upgrade your company branding with 500+ premium matte or velvet-touch visiting cards.', 20, 2500, to_timestamp(1793673730.214), TRUE) ON CONFLICT (code) DO NOTHING;
INSERT INTO promotions (promotion_id, code, title, description, discount_percentage, min_order_amount, valid_until, is_active) VALUES (3, 'FLYER15', '15% Off A5 & A4 Event Leaflets', 'High-speed double-sided Heidelberg print output on runs of 500 pcs or more.', 15, 3000, to_timestamp(1793673730.214), TRUE) ON CONFLICT (code) DO NOTHING;
INSERT INTO promotions (promotion_id, code, title, description, discount_percentage, min_order_amount, valid_until, is_active) VALUES (4, 'PRINT20', '20% Off Corporate Stationery', 'Special promotional discount on business cards, letterheads, and envelopes.', 20, 2000, to_timestamp(1793673730.214), TRUE) ON CONFLICT (code) DO NOTHING;

-- Seed: Orders & Order Items
INSERT INTO orders (order_id, customer_id, order_date, total_amount, delivery_method, delivery_address_id, payment_method, payment_status, order_status) VALUES (1, 2, to_timestamp(1790995330.213), 11840, 'Home Delivery', 1, 'Credit / Debit Card', 'PAID', 'PRINTING') ON CONFLICT (order_id) DO NOTHING;
INSERT INTO orders (order_id, customer_id, order_date, total_amount, delivery_method, delivery_address_id, payment_method, payment_status, order_status) VALUES (2, 2, to_timestamp(1790822530.213), 8910, 'Store Pickup', NULL, 'Cash on Delivery', 'PENDING', 'READY') ON CONFLICT (order_id) DO NOTHING;

INSERT INTO order_items (order_item_id, order_id, productId, quantity, unit_price, customisation_details, artwork_file_path, subtotal) VALUES (1, 1, 1, 2, 2750, 'Size: 90x54mm Standard (100 pcs) | Material: 400gsm Velvet Soft-Touch | Text: Kasun Perera - Managing Director', '', 5500) ON CONFLICT (order_item_id) DO NOTHING;
INSERT INTO order_items (order_item_id, order_id, productId, quantity, unit_price, customisation_details, artwork_file_path, subtotal) VALUES (2, 1, 5, 3, 1650, 'Size: A5 Double-Sided (100 pcs) | Material: 150gsm Silk Matte | Text: Colombo Tech Expo 2026 Handout', '', 4950) ON CONFLICT (order_item_id) DO NOTHING;
INSERT INTO order_items (order_item_id, order_id, productId, quantity, unit_price, customisation_details, artwork_file_path, subtotal) VALUES (3, 2, 9, 1, 8250, 'Size: Standard (2.5x6.5 ft / 85x200cm) | Material: 440gsm Matte Blockout Vinyl | Text: BMICH Booth #B14 Standee', '', 8250) ON CONFLICT (order_item_id) DO NOTHING;

-- Seed: Notifications
INSERT INTO notifications (notification_id, user_id, title, message, timestamp, is_read, type) VALUES (1, 2, 'Order #1 is now Printing 🖨️', 'Your 400gsm Velvet Business Cards and A5 Silk Flyers are currently on the Heidelberg offset press.', to_timestamp(1791078130.213), FALSE, 'ORDER') ON CONFLICT (notification_id) DO NOTHING;
INSERT INTO notifications (notification_id, user_id, title, message, timestamp, is_read, type) VALUES (2, 2, 'Order #2 Ready for Pickup ✅', 'Your Roll-up Pull Banner is packed and ready for collection at our Colombo 03 Production Hub.', to_timestamp(1791074530.213), FALSE, 'ORDER') ON CONFLICT (notification_id) DO NOTHING;
