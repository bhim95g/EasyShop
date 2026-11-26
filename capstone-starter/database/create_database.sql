USE sys;

# ---------------------------------------------------------------------- #
# Target DBMS:           MySQL                                           #
# Project name:          EasyShop                                        #
# ---------------------------------------------------------------------- #
DROP DATABASE IF EXISTS easyshop;

CREATE DATABASE IF NOT EXISTS easyshop;

USE easyshop;

# ---------------------------------------------------------------------- #
# Tables                                                                 #
# ---------------------------------------------------------------------- #

CREATE TABLE users (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id)
);

CREATE TABLE profiles (
    user_id INT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(200) NOT NULL,
    address VARCHAR(200) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE categories (
    category_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    PRIMARY KEY (category_id)
);

CREATE TABLE products (
    product_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category_id INT NOT NULL,
    description TEXT,
    color VARCHAR(20),
    image_url VARCHAR(200),
    stock INT NOT NULL DEFAULT 0,
    featured BOOL NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

CREATE TABLE orders (
    order_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    date DATETIME NOT NULL,
    address VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip VARCHAR(20) NOT NULL,
    shipping_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (order_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE order_line_items (
    order_line_item_id INT NOT NULL AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    sales_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    discount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (order_line_item_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- new tables
CREATE TABLE shopping_cart (
	user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);


/*  INSERT Users  */
INSERT INTO users (username, hashed_password, role) 
VALUES  ('user','$2a$10$NkufUPF3V8dEPSZeo1fzHe9ScBu.LOay9S3N32M84yuUM2OJYEJ/.','ROLE_USER'),
        ('admin','$2a$10$lfQi9jSfhZZhfS6/Kyzv3u3418IgnWXWDQDk7IbcwlCFPgxg9Iud2','ROLE_ADMIN'),
        ('george','$2a$10$lfQi9jSfhZZhfS6/Kyzv3u3418IgnWXWDQDk7IbcwlCFPgxg9Iud2','ROLE_USER');

/* INSERT Profiles */
INSERT INTO profiles (user_id, first_name, last_name, phone, email, address, city, state, zip)
VALUES  (1, 'Joe', 'Joesephus', '800-555-1234', 'joejoesephus@email.com', '789 Oak Avenue', 'Dallas', 'TX', '75051'),
        (2, 'Adam', 'Admamson', '800-555-1212', 'aaadamson@email.com', '456 Elm Street','Dallas','TX','75052'),
        (3, 'George', 'Jetson', '800-555-9876', 'george.jetson@email.com', '123 Birch Parkway','Dallas','TX','75051')     ;

/* INSERT Categories */
INSERT INTO categories (name, description) 
VALUES  ('Electronics', 'Smart tech, creator gear and gadgets that keep up with you.'),
        ('Fashion', 'Athleisure, streetwear and technical layers built for everyday movement.'),
        ('Home & Kitchen', 'Coffee-bar upgrades, smart appliances and kitchen tools worth showing off.'),
        ('Outdoor & Adventure', 'Gear for trails, water, camp and city rides.'),
        ('Fitness & Wellness', 'Train harder, recover smarter and feel better at home.');

/* INSERT Products */
-- electronics
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('AI Smart Glasses', 299.00, 1, 'Open-ear audio, a hands-free camera and a voice assistant in everyday frames.', 'ai-smart-glasses.jpg', 25, 1, 'Black'),
        ('Smart Health Ring', 249.00, 1, 'Tracks sleep, heart rate and recovery from a titanium ring with a 7-day battery.', 'smart-health-ring.jpg', 40, 1, 'Titanium'),
        ('4K Mini Camera Drone', 459.00, 1, 'Folds to pocket size and films stabilized 4K with obstacle sensing and auto follow.', '4k-mini-camera-drone.jpg', 18, 1, 'Gray'),
        ('Pocket Gimbal Vlog Camera', 389.00, 1, 'A palm-sized 3-axis camera that keeps every walk-and-talk smooth.', 'pocket-gimbal-vlog-camera.jpg', 22, 0, 'Black'),
        ('5K Action Camera', 349.00, 1, 'Waterproof to 33 ft with horizon lock and front and rear screens.', '5k-action-camera.jpg', 30, 0, 'Black'),
        ('Noise-Cancelling Headphones', 279.00, 1, 'Adaptive noise cancelling, 40-hour battery and plush memory-foam cushions.', 'noise-cancelling-headphones.jpg', 45, 1, 'White'),
        ('Open-Ear Sport Earbuds', 129.00, 1, 'Secure ear hooks and sweatproof sound that still lets you hear traffic.', 'open-ear-sport-earbuds.jpg', 60, 0, 'Black'),
        ('GPS Adventure Watch', 399.00, 1, 'Offline maps, 20-day battery and training readiness on a sapphire screen.', 'gps-adventure-watch.jpg', 28, 1, 'Black'),
        ('Portable Power Station', 699.00, 1, 'A 1,000 Wh battery that runs a fridge, laptop and lights off-grid.', 'portable-power-station.jpg', 12, 0, 'Gray'),
        ('Foldable Solar Panel', 229.00, 1, 'A 100 W panel that folds like a book and charges power stations in the sun.', 'foldable-solar-panel.jpg', 20, 0, 'Black'),
        ('Waterproof Party Speaker', 179.00, 1, 'Huge 360-degree sound, a floating design and lights that pulse to the beat.', 'waterproof-party-speaker.jpg', 35, 0, 'Black'),
        ('Mini Smart Projector', 399.00, 1, 'A 1080p projector the size of a soda can for movie nights anywhere.', 'mini-smart-projector.jpg', 16, 1, 'Silver'),
        ('Instant Print Camera', 99.00, 1, 'Snap and print credit-card-size photos in seconds.', 'instant-print-camera.jpg', 50, 0, 'Mint'),
        ('Retro Handheld Game Console', 149.00, 1, 'A pocket console with a bright screen for classic-style games on the go.', 'retro-handheld-game-console.jpg', 0, 0, 'White'),
        ('VR Fitness Headset', 499.00, 1, 'Standalone VR with workouts, boxing and rhythm games that make you sweat.', 'vr-fitness-headset.jpg', 14, 1, 'White'),
        ('Creator Wireless Mic Kit', 199.00, 1, 'Two clip-on mics and a receiver for crisp audio on phone or camera.', 'creator-wireless-mic-kit.jpg', 40, 0, 'Black'),
        ('3-in-1 Wireless Charging Stand', 89.00, 1, 'Charges your phone, watch and earbuds from one sleek stand.', '3-in-1-wireless-charging-stand.jpg', 55, 0, 'White'),
        ('Magnetic Power Bank', 59.00, 1, 'Snaps onto your phone and adds a full charge on the go.', 'magnetic-power-bank.jpg', 80, 0, 'Black'),
        ('Ultralight Creator Laptop', 1299.00, 1, 'A 14-inch laptop with a color-accurate screen and all-day battery.', 'ultralight-creator-laptop.jpg', 15, 0, 'Silver'),
        ('Mechanical RGB Keyboard', 129.00, 1, 'Hot-swappable switches, a metal frame and per-key lighting.', 'mechanical-rgb-keyboard.jpg', 30, 0, 'Black');

-- fashion
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Oversized Heavyweight Hoodie', 89.00, 2, 'Thick brushed cotton, dropped shoulders and a boxy fit.', 'oversized-heavyweight-hoodie.jpg', 70, 1, 'Bone'),
        ('Tech Fleece Joggers', 95.00, 2, 'Lightweight warmth with a tapered leg and zip pockets.', 'tech-fleece-joggers.jpg', 60, 1, 'Charcoal'),
        ('Seamless Sculpt Leggings', 78.00, 2, 'High-rise, squat-proof leggings with a buttery second-skin feel.', 'seamless-sculpt-leggings.jpg', 80, 1, 'Black'),
        ('Women''s Training Set', 98.00, 2, 'A supportive sports bra and matching bike shorts in sweat-wicking knit.', 'women-s-training-set.jpg', 45, 0, 'Sage'),
        ('Carbon-Plate Running Shoes', 220.00, 2, 'Super-foam and a full carbon plate for race-day speed.', 'carbon-plate-running-shoes.jpg', 25, 1, 'Volt'),
        ('Retro Court Sneakers', 110.00, 2, 'Clean leather low-tops inspired by 80s tennis courts.', 'retro-court-sneakers.jpg', 55, 0, 'White'),
        ('Waterproof Shell Jacket', 249.00, 2, 'A 3-layer rain shell that packs into its own pocket.', 'waterproof-shell-jacket.jpg', 30, 1, 'Black'),
        ('Packable Puffer Jacket', 169.00, 2, 'Featherweight insulation that squishes into a stuff sack.', 'packable-puffer-jacket.jpg', 35, 0, 'Olive'),
        ('Utility Cargo Pants', 88.00, 2, 'Relaxed stretch-ripstop cargos with plenty of pockets.', 'utility-cargo-pants.jpg', 50, 0, 'Khaki'),
        ('Graphic Oversized Tee', 42.00, 2, 'Heavy 240 gsm cotton with a bold back print.', 'graphic-oversized-tee.jpg', 90, 0, 'White'),
        ('Bamboo Lounge Set', 110.00, 2, 'A cloud-soft bamboo top and pants for slow mornings.', 'bamboo-lounge-set.jpg', 40, 0, 'Oat'),
        ('Merino Quarter-Zip', 128.00, 2, 'Temperature-regulating merino for runs, hikes and the office.', 'merino-quarter-zip.jpg', 35, 0, 'Navy'),
        ('Varsity Bomber Jacket', 159.00, 2, 'Wool-blend body, leather-look sleeves and ribbed trims.', 'varsity-bomber-jacket.jpg', 20, 0, 'Black'),
        ('Wide-Leg Vintage Jeans', 98.00, 2, 'Rigid denim with a high waist and 90s wide leg.', 'wide-leg-vintage-jeans.jpg', 45, 0, 'Light Wash'),
        ('Crossbody Sling Bag', 58.00, 2, 'Water-resistant hands-free carry for phone, keys and wallet.', 'crossbody-sling-bag.jpg', 75, 1, 'Black'),
        ('Polarized Sport Sunglasses', 129.00, 2, 'Wraparound lenses that cut glare on the road and water.', 'polarized-sport-sunglasses.jpg', 40, 0, 'Black'),
        ('Trail Running Cap', 35.00, 2, 'Ultralight, quick-dry and packable with a soft brim.', 'trail-running-cap.jpg', 85, 0, 'Black'),
        ('Performance Crew Socks 3-Pack', 24.00, 2, 'Cushioned, blister-resistant socks with arch support.', 'performance-crew-socks-3-pack.jpg', 120, 0, 'White'),
        ('Pleated Tennis Skirt', 64.00, 2, 'Flippy pleats with built-in shorts and a ball pocket.', 'pleated-tennis-skirt.jpg', 50, 0, 'White'),
        ('Cropped Training Tee', 45.00, 2, 'Breathable, boxy and cropped for workouts or weekends.', 'cropped-training-tee.jpg', 70, 0, 'White');

-- home & kitchen
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Home Espresso Machine', 599.00, 3, 'A built-in grinder and steam wand for cafe drinks at home.', 'home-espresso-machine.jpg', 18, 1, 'Steel'),
        ('Countertop Pizza Oven', 399.00, 3, 'Reaches 750 degrees for crisp, leopard-spotted pizza in 90 seconds.', 'countertop-pizza-oven.jpg', 15, 1, 'Black'),
        ('Dual-Basket Air Fryer', 179.00, 3, 'Two baskets cook two foods at once and finish at the same time.', 'dual-basket-air-fryer.jpg', 40, 1, 'Black'),
        ('Frozen Treat Maker', 229.00, 3, 'Turns frozen pints into ice cream, sorbet or protein ice cream.', 'frozen-treat-maker.jpg', 22, 1, 'Silver'),
        ('Gooseneck Electric Kettle', 89.00, 3, 'Precise temperature control and a slow pour for perfect brews.', 'gooseneck-electric-kettle.jpg', 45, 0, 'Matte Black'),
        ('Pour-Over Coffee Set', 69.00, 3, 'A glass carafe, dripper and filters for a slow morning ritual.', 'pour-over-coffee-set.jpg', 50, 0, 'Clear'),
        ('Smart Indoor Herb Garden', 149.00, 3, 'Grow basil and greens year-round under automatic grow lights.', 'smart-indoor-herb-garden.jpg', 25, 0, 'White'),
        ('Electric Food Recycler', 349.00, 3, 'Turns food scraps into dry, odor-free compost overnight.', 'electric-food-recycler.jpg', 10, 0, 'Gray'),
        ('Enameled Dutch Oven', 129.00, 3, 'Cast iron that goes from stovetop to oven to table.', 'enameled-dutch-oven.jpg', 35, 0, 'Red'),
        ('Damascus Chef Knife', 139.00, 3, 'A 67-layer steel blade with a razor edge and walnut handle.', 'damascus-chef-knife.jpg', 30, 1, 'Steel'),
        ('Walnut Cutting Board', 79.00, 3, 'A thick end-grain walnut board that is gentle on knives.', 'walnut-cutting-board.jpg', 40, 0, 'Walnut'),
        ('Personal Smoothie Blender', 89.00, 3, 'Blend straight into a to-go cup and head out the door.', 'personal-smoothie-blender.jpg', 60, 0, 'White'),
        ('Pro Stand Mixer', 399.00, 3, 'A powerful motor and 5-quart bowl for bread, cookies and more.', 'pro-stand-mixer.jpg', 12, 0, 'Sage'),
        ('Insulated Steel Water Bottle', 39.00, 3, 'Keeps drinks ice-cold for 24 hours with a chug lid.', 'insulated-steel-water-bottle.jpg', 150, 1, 'Black'),
        ('Travel Tumbler 40 oz', 45.00, 3, 'A handle, straw lid and car-cup-holder base for all-day sipping.', 'travel-tumbler-40-oz.jpg', 0, 0, 'Stone'),
        ('Stoneware Mug Set', 48.00, 3, 'Four speckled handmade-look mugs for coffee bar mornings.', 'stoneware-mug-set.jpg', 55, 0, 'Speckled White'),
        ('Sunset Ambient Lamp', 59.00, 3, 'Projects a warm golden-hour glow onto any wall.', 'sunset-ambient-lamp.jpg', 45, 0, 'Black'),
        ('Glass Pantry Canister Set', 54.00, 3, 'Airtight glass jars with bamboo lids for a tidy pantry.', 'glass-pantry-canister-set.jpg', 40, 0, 'Clear'),
        ('Water Filter Pitcher', 39.00, 3, 'Cleaner-tasting water from the fridge in seconds.', 'water-filter-pitcher.jpg', 70, 0, 'White'),
        ('Stoneware Dinner Set', 129.00, 3, 'A 12-piece set of plates and bowls with a matte glaze.', 'stoneware-dinner-set.jpg', 20, 0, 'Sand');

-- outdoor & adventure
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Ultralight 2-Person Tent', 349.00, 4, 'Under 3 lb, freestanding and storm-ready for fast backpacking.', 'ultralight-2-person-tent.jpg', 18, 1, 'Green'),
        ('Inflatable Paddle Board', 449.00, 4, 'A stable 10''6" board with paddle, pump and backpack.', 'inflatable-paddle-board.jpg', 12, 1, 'Blue'),
        ('40L Hiking Backpack', 189.00, 4, 'A ventilated back panel and smart pockets for weekend trips.', '40l-hiking-backpack.jpg', 30, 0, 'Black'),
        ('Down Sleeping Bag', 279.00, 4, 'Lofty 650-fill down rated to 20 degrees.', 'down-sleeping-bag.jpg', 20, 0, 'Orange'),
        ('Portable Camp Stove', 69.00, 4, 'Boils water in 3 minutes and folds into a pocket.', 'portable-camp-stove.jpg', 45, 0, 'Silver'),
        ('Travel Hammock', 49.00, 4, 'Parachute nylon with tree straps and a 400 lb capacity.', 'travel-hammock.jpg', 60, 0, 'Teal'),
        ('Carbon Trekking Poles', 119.00, 4, 'Featherweight carbon poles with cork grips that fold in seconds.', 'carbon-trekking-poles.jpg', 35, 0, 'Black'),
        ('Rechargeable Headlamp', 39.00, 4, '400 lumens, red light mode and USB-C charging.', 'rechargeable-headlamp.jpg', 90, 0, 'Black'),
        ('Folding Camp Chair', 79.00, 4, 'Packs small and sets up in seconds for camp or concerts.', 'folding-camp-chair.jpg', 40, 0, 'Black'),
        ('Smokeless Fire Pit', 299.00, 4, 'Steel airflow design for a big flame with barely any smoke.', 'smokeless-fire-pit.jpg', 14, 1, 'Steel'),
        ('Rolling Hard Cooler', 249.00, 4, 'Keeps ice for 5 days and rolls over rough ground.', 'rolling-hard-cooler.jpg', 16, 0, 'White'),
        ('Electric Commuter Scooter', 649.00, 4, 'Folds in 3 seconds and rides 25 miles per charge.', 'electric-commuter-scooter.jpg', 10, 1, 'Black'),
        ('MIPS Bike Helmet', 129.00, 4, 'Lightweight protection with a rotational-impact liner.', 'mips-bike-helmet.jpg', 40, 0, 'Matte Black'),
        ('All-Mountain Snowboard', 499.00, 4, 'A playful twin shape that rips groomers and powder.', 'all-mountain-snowboard.jpg', 8, 0, 'Black'),
        ('Inflatable Kayak', 399.00, 4, 'Fits in a car trunk, inflates in 10 minutes and tracks straight.', 'inflatable-kayak.jpg', 10, 0, 'Orange'),
        ('Camping Lantern', 49.00, 4, 'Bright, dimmable and doubles as a phone charger.', 'camping-lantern.jpg', 60, 0, 'Green'),
        ('Climbing Shoes', 159.00, 4, 'Aggressive downturn and sticky rubber for steep routes.', 'climbing-shoes.jpg', 25, 0, 'Yellow'),
        ('Compact Binoculars', 149.00, 4, 'Sharp 10x magnification in a pocket-size body.', 'compact-binoculars.jpg', 30, 0, 'Black'),
        ('Waterproof Dry Bag', 29.00, 4, 'Roll-top protection for gear on boats, beaches and rainy rides.', 'waterproof-dry-bag.jpg', 100, 0, 'Yellow'),
        ('Cruiser Skateboard', 119.00, 4, 'Soft wheels and a kicktail for smooth city cruising.', 'cruiser-skateboard.jpg', 30, 0, 'Natural');

-- fitness & wellness
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Adjustable Dumbbell Set', 399.00, 5, 'Dial from 5 to 52.5 lb, replacing 15 pairs of weights.', 'adjustable-dumbbell-set.jpg', 15, 1, 'Black'),
        ('Smart Jump Rope', 59.00, 5, 'Counts reps and calories and syncs with your phone.', 'smart-jump-rope.jpg', 70, 0, 'Black'),
        ('Cast Iron Kettlebell 16 kg', 69.00, 5, 'A wide handle for swings, snatches and Hyrox-style training.', 'cast-iron-kettlebell-16-kg.jpg', 40, 0, 'Black'),
        ('Pro Grip Yoga Mat', 89.00, 5, '6 mm cushioning with a grippy surface that holds even when sweaty.', 'pro-grip-yoga-mat.jpg', 60, 1, 'Plum'),
        ('Percussion Massage Gun', 199.00, 5, 'Deep-tissue relief with 5 heads and a whisper-quiet motor.', 'percussion-massage-gun.jpg', 35, 1, 'Gray'),
        ('High-Density Foam Roller', 35.00, 5, 'Firm texture to roll out tight legs and back.', 'high-density-foam-roller.jpg', 80, 0, 'Black'),
        ('Resistance Band Set', 39.00, 5, 'Five stackable bands with handles and a door anchor.', 'resistance-band-set.jpg', 90, 0, 'Multi'),
        ('Weighted Vest 20 lb', 129.00, 5, 'Adds load to walks, runs and calisthenics without bouncing.', 'weighted-vest-20-lb.jpg', 25, 0, 'Black'),
        ('Doorway Pull-Up Bar', 49.00, 5, 'No-screw install for pull-ups, chin-ups and hanging leg raises.', 'doorway-pull-up-bar.jpg', 50, 0, 'Black'),
        ('Magnetic Rowing Machine', 799.00, 5, 'Smooth, quiet full-body cardio that folds upright.', 'magnetic-rowing-machine.jpg', 8, 1, 'Black'),
        ('Under-Desk Walking Pad', 349.00, 5, 'A slim treadmill that slides under the desk or sofa.', 'under-desk-walking-pad.jpg', 14, 0, 'Gray'),
        ('Indoor Cycling Bike', 899.00, 5, 'A heavy flywheel, magnetic resistance and a tablet holder.', 'indoor-cycling-bike.jpg', 6, 0, 'Black'),
        ('Cold Plunge Tub', 299.00, 5, 'An insulated inflatable ice bath for post-workout recovery.', 'cold-plunge-tub.jpg', 10, 1, 'Black'),
        ('Protein Shaker Bottle', 19.00, 5, 'A leakproof shaker that mixes smooth with no clumps.', 'protein-shaker-bottle.jpg', 150, 0, 'Black'),
        ('Gym Duffel Bag', 79.00, 5, 'A shoe tunnel, wet pocket and room for a full kit.', 'gym-duffel-bag.jpg', 45, 0, 'Black'),
        ('Boxing Gloves', 69.00, 5, 'Multi-layer foam padding for bag work and sparring.', 'boxing-gloves.jpg', 40, 0, 'Red'),
        ('Weightlifting Belt', 59.00, 5, 'A 4-inch leather belt for heavy squats and deadlifts.', 'weightlifting-belt.jpg', 30, 0, 'Black'),
        ('Meditation Cushion', 49.00, 5, 'A buckwheat-filled cushion for comfortable stillness.', 'meditation-cushion.jpg', 45, 0, 'Charcoal'),
        ('Foam Plyo Box', 149.00, 5, 'A soft 3-in-1 box for jumps, step-ups and dips.', 'foam-plyo-box.jpg', 18, 0, 'Black'),
        ('Battle Rope', 89.00, 5, 'A 30 ft heavy rope for brutal, fun conditioning.', 'battle-rope.jpg', 20, 0, 'Black');

/* INSERT Products: new drop (product_id 101-200) */
-- new drop: electronics
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Wireless Gaming Headset', 149.00, 1, 'Low-latency wireless sound, a flip-to-mute mic and 30 hours of battery for long sessions.', 'wireless-gaming-headset.jpg', 55, 1, 'Black'),
        ('E-Ink Reader', 139.00, 1, 'A glare-free 7-inch screen with warm light and weeks of battery for reading anywhere.', 'e-ink-reader.jpg', 60, 0, 'Black'),
        ('Smart Video Doorbell', 179.00, 1, 'See, hear and talk to visitors from your phone, with HD video and night vision.', 'smart-video-doorbell.jpg', 35, 0, 'Black'),
        ('Mesh Wi-Fi 3-Pack', 299.00, 1, 'Three small routers that cover your whole home in fast, steady Wi-Fi.', 'mesh-wi-fi-3-pack.jpg', 25, 0, 'White'),
        ('15.6-Inch Portable Monitor', 229.00, 1, 'A slim USB-C second screen that turns any table into a two-screen desk.', '15-6-inch-portable-monitor.jpg', 30, 0, 'Silver'),
        ('Noise-Cancelling Earbuds', 199.00, 1, 'Pocket-size quiet with adaptive noise cancelling and a wireless charging case.', 'noise-cancelling-earbuds.jpg', 70, 1, 'White'),
        ('Smart Home Speaker', 99.00, 1, 'Room-filling sound with a built-in voice assistant for music, timers and lights.', 'smart-home-speaker.jpg', 80, 0, 'Orange'),
        ('Digital Photo Frame', 129.00, 1, 'A 10-inch HD frame that shows new family photos sent straight from your phone.', 'digital-photo-frame.jpg', 40, 0, 'Black'),
        ('Mirrorless Camera Kit', 1099.00, 1, 'A 24 MP mirrorless body with a zoom lens, 4K video and fast eye-tracking focus.', 'mirrorless-camera-kit.jpg', 12, 0, 'Black'),
        ('Wireless Game Controller', 69.00, 1, 'Textured grips, responsive triggers and Bluetooth for PC, phone and TV.', 'wireless-game-controller.jpg', 90, 0, 'Camo'),
        ('Item Tracker 4-Pack', 79.00, 1, 'Clip one to your keys, bag or bike and find it on a map with your phone.', 'item-tracker-4-pack.jpg', 100, 0, 'White'),
        ('Silent Wireless Mouse', 39.00, 1, 'Near-silent clicks, a comfortable shape and a battery that lasts for months.', 'silent-wireless-mouse.jpg', 110, 0, 'White'),
        ('4K Dash Cam', 119.00, 1, 'Records the road in 4K with night vision and automatic crash saving.', '4k-dash-cam.jpg', 40, 0, 'Black'),
        ('Bluetooth Record Player', 199.00, 1, 'A belt-drive turntable with built-in speakers that also streams to Bluetooth speakers.', 'bluetooth-record-player.jpg', 22, 0, 'Walnut'),
        ('Smart Bird Feeder Camera', 179.00, 1, 'Snaps photos of every visitor and tells you which bird just stopped by.', 'smart-bird-feeder-camera.jpg', 9, 0, 'Green'),
        ('Smart LED Light Strip', 49.00, 1, '16 million colors and music sync to change the mood of any room from your phone.', 'smart-led-light-strip.jpg', 120, 0, 'Multicolor'),
        ('Studio Desk Speakers', 249.00, 1, 'A pair of powered bookshelf speakers with rich bass for music and editing.', 'studio-desk-speakers.jpg', 20, 0, 'Black'),
        ('4K Streaming Webcam', 129.00, 1, 'Sharp 4K video, automatic light correction and dual mics for calls and streams.', '4k-streaming-webcam.jpg', 45, 0, 'Black'),
        ('Drawing Tablet with Pen', 349.00, 1, 'A 13-inch pen display with tilt support for sketching, design and notes.', 'drawing-tablet-with-pen.jpg', 18, 0, 'Black'),
        ('Smart Thermostat', 199.00, 1, 'Learns your schedule, saves energy and lets you change the temperature from your phone.', 'smart-thermostat.jpg', 30, 0, 'Silver');

-- new drop: fashion
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Trail Running Shoes', 150.00, 2, 'Grippy lugs, a rock plate and a cushioned ride for muddy, rocky trails.', 'trail-running-shoes.jpg', 45, 1, 'Black'),
        ('Cozy Shearling Slippers', 55.00, 2, 'Plush faux-shearling slippers with a grippy sole for chilly mornings.', 'cozy-shearling-slippers.jpg', 85, 0, 'Oat'),
        ('Classic Canvas Sneakers', 70.00, 2, 'A classic low-top in sturdy canvas with a grippy rubber sole.', 'classic-canvas-sneakers.jpg', 80, 0, 'White'),
        ('High-Support Sports Bra', 48.00, 2, 'Molded cups and wide straps for running and high-impact workouts.', 'high-support-sports-bra.jpg', 75, 0, 'Pastel Blue'),
        ('2-in-1 Running Shorts', 55.00, 2, 'Light shorts with a built-in liner and a zip pocket for keys.', '2-in-1-running-shorts.jpg', 90, 0, 'Yellow'),
        ('Retro Windbreaker', 120.00, 2, 'A light shell that blocks wind and drizzle, with a stowaway hood.', 'retro-windbreaker.jpg', 40, 0, 'Black'),
        ('Ribbed Knit Beanie', 28.00, 2, 'A soft, warm rib-knit beanie with a fold-over cuff.', 'ribbed-knit-beanie.jpg', 110, 0, 'Charcoal'),
        ('Canvas Tote Bag', 38.00, 2, 'A roomy heavyweight canvas tote with an inside zip pocket.', 'canvas-tote-bag.jpg', 85, 0, 'Black'),
        ('Slim Leather Card Wallet', 35.00, 2, 'Full-grain leather that holds six cards and folded cash.', 'slim-leather-card-wallet.jpg', 95, 0, 'Tan'),
        ('Chunky Knit Cardigan', 98.00, 2, 'An oversized knit cardigan with deep pockets and wooden buttons.', 'chunky-knit-cardigan.jpg', 35, 0, 'Heather Gray'),
        ('Linen Button-Up Shirt', 68.00, 2, 'Breathable washed linen for hot days and relaxed evenings.', 'linen-button-up-shirt.jpg', 60, 0, 'Sand'),
        ('Retro Track Jacket', 90.00, 2, 'A zip-up track jacket with a stand collar and a relaxed fit.', 'retro-track-jacket.jpg', 50, 1, 'Red'),
        ('Ribbed Tank Top', 28.00, 2, 'A stretchy ribbed tank that layers under everything.', 'ribbed-tank-top.jpg', 130, 0, 'White'),
        ('Flared Yoga Pants', 72.00, 2, 'High-rise, buttery-soft pants with a retro flare.', 'flared-yoga-pants.jpg', 70, 0, 'Black'),
        ('Quilted Puffer Vest', 110.00, 2, 'Light insulated warmth for your core without the bulk.', 'quilted-puffer-vest.jpg', 45, 0, 'Green'),
        ('Denim Trucker Jacket', 98.00, 2, 'A rigid-denim classic that softens and fades the more you wear it.', 'denim-trucker-jacket.jpg', 55, 0, 'Indigo'),
        ('Suede Chelsea Boots', 160.00, 2, 'Pull-on suede boots with elastic sides and a lug sole.', 'suede-chelsea-boots.jpg', 30, 0, 'Brown'),
        ('Everyday Leather Backpack', 160.00, 2, 'A minimal leather backpack with a padded laptop sleeve.', 'everyday-leather-backpack.jpg', 25, 0, 'Brown'),
        ('Bucket Hat', 32.00, 2, 'A washed-cotton bucket hat for sun, festivals and bad hair days.', 'bucket-hat.jpg', 100, 0, 'White'),
        ('Thermal Base Layer Set', 79.00, 2, 'A warm, quick-drying top and leggings for cold-weather runs and ski days.', 'thermal-base-layer-set.jpg', 6, 0, 'Charcoal');

-- new drop: home & kitchen
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Robot Vacuum', 449.00, 3, 'Maps your home, avoids cords and empties itself into its base for weeks.', 'robot-vacuum.jpg', 20, 1, 'White'),
        ('Cold Brew Coffee Maker', 39.00, 3, 'Steep overnight in a fine-mesh filter for smooth, low-acid cold brew.', 'cold-brew-coffee-maker.jpg', 70, 0, 'Clear'),
        ('Cast Iron Skillet', 45.00, 3, 'A pre-seasoned 12-inch skillet that sears, bakes and lasts for generations.', 'cast-iron-skillet.jpg', 80, 0, 'Black'),
        ('Stainless Steel Pan Set', 149.00, 3, 'Three tri-ply pans that heat evenly and go from stove to oven.', 'stainless-steel-pan-set.jpg', 35, 0, 'Steel'),
        ('Matcha Whisk Set', 35.00, 3, 'A bamboo whisk, scoop and bowl for frothy, cafe-style matcha.', 'matcha-whisk-set.jpg', 60, 0, 'Natural'),
        ('Sourdough Baking Kit', 59.00, 3, 'A proofing basket, scoring blade and dough scraper for crusty homemade loaves.', 'sourdough-baking-kit.jpg', 40, 0, 'Natural'),
        ('Cocktail Shaker Set', 49.00, 3, 'A shaker, jigger, strainer and muddler for bar-quality drinks at home.', 'cocktail-shaker-set.jpg', 55, 0, 'Steel'),
        ('Meal Prep Container Set', 34.00, 3, 'Ten leak-proof, microwave-safe containers with snap-lock lids for the week ahead.', 'meal-prep-container-set.jpg', 90, 0, 'Clear'),
        ('Smart Meat Thermometer', 79.00, 3, 'A wireless probe that pings your phone when steak, chicken or roasts are done.', 'smart-meat-thermometer.jpg', 50, 0, 'Black'),
        ('HEPA Air Purifier', 199.00, 3, 'A quiet true-HEPA purifier that clears dust, pollen and smoke from large rooms.', 'hepa-air-purifier.jpg', 28, 0, 'White'),
        ('Essential Oil Diffuser', 39.00, 3, 'A wood-look ultrasonic diffuser with a soft glow and auto shut-off.', 'essential-oil-diffuser.jpg', 85, 0, 'Wood'),
        ('Belgian Waffle Maker', 69.00, 3, 'Deep-pocket waffles in five minutes with even, golden browning.', 'belgian-waffle-maker.jpg', 45, 0, 'Steel'),
        ('Smart Rice Cooker', 99.00, 3, 'Fluffy rice, grains and oatmeal with a keep-warm setting and a timer.', 'smart-rice-cooker.jpg', 40, 0, 'White'),
        ('Linen Sheet Set', 189.00, 3, 'Stonewashed linen sheets that get softer with every wash.', 'linen-sheet-set.jpg', 25, 0, 'Natural'),
        ('Chunky Knit Throw Blanket', 89.00, 3, 'A hand-knit throw made of thick, cozy yarn for the couch.', 'chunky-knit-throw-blanket.jpg', 35, 1, 'Cream'),
        ('Soy Candle Trio', 42.00, 3, 'Three hand-poured soy candles in fig, cedar and sea salt.', 'soy-candle-trio.jpg', 100, 0, 'Amber'),
        ('Playful Ceramic Planters', 58.00, 3, 'Hand-glazed planters with cheerful designs and drainage holes. Set of three.', 'playful-ceramic-planters.jpg', 50, 0, 'Multicolor'),
        ('Handheld Milk Frother', 25.00, 3, 'Whips silky foam for lattes, matcha and hot chocolate in seconds.', 'handheld-milk-frother.jpg', 150, 0, 'Black'),
        ('Electric Citrus Juicer', 49.00, 3, 'Presses fresh orange, lemon and grapefruit juice with an auto-reversing cone.', 'electric-citrus-juicer.jpg', 8, 0, 'Silver'),
        ('Granite Mortar and Pestle', 39.00, 3, 'Heavy granite for crushing spices, pesto and fresh guacamole.', 'granite-mortar-and-pestle.jpg', 60, 0, 'Gray');

-- new drop: outdoor & adventure
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Waterproof Hiking Boots', 189.00, 4, 'Waterproof leather boots with ankle support and grippy rubber soles.', 'waterproof-hiking-boots.jpg', 40, 1, 'Brown'),
        ('Trail E-Bike', 1899.00, 4, 'A pedal-assist mountain bike with a 50-mile battery for climbs you used to walk.', 'trail-e-bike.jpg', 6, 0, 'Black'),
        ('Insulated Sleeping Pad', 129.00, 4, 'An ultralight inflatable pad that keeps the cold ground away.', 'insulated-sleeping-pad.jpg', 45, 0, 'Orange'),
        ('Travel Fishing Rod Kit', 89.00, 4, 'A telescoping rod and reel that pack into a backpack-size case.', 'travel-fishing-rod-kit.jpg', 35, 0, 'Black'),
        ('Snorkel Mask Set', 59.00, 4, 'A dry-top snorkel and anti-fog mask for reef days.', 'snorkel-mask-set.jpg', 50, 0, 'Blue'),
        ('Soft-Top Surfboard 8 ft', 299.00, 4, 'A stable, forgiving foam board made for learning to catch waves.', 'soft-top-surfboard-8-ft.jpg', 15, 0, 'Blue'),
        ('Anti-Fog Ski Goggles', 149.00, 4, 'Wide magnetic lenses with anti-fog coating for clear runs in any light.', 'anti-fog-ski-goggles.jpg', 30, 0, 'Black'),
        ('Waterproof Ski Gloves', 69.00, 4, 'Insulated, waterproof gloves with touchscreen fingertips.', 'waterproof-ski-gloves.jpg', 55, 0, 'Black'),
        ('Baseplate Navigation Compass', 29.00, 4, 'A liquid-filled compass with a map ruler and glow-in-the-dark markings for when there''s no signal.', 'baseplate-navigation-compass.jpg', 75, 0, 'Black'),
        ('Disc Golf Starter Set', 49.00, 4, 'A driver, mid-range and putter plus a bag to play any course.', 'disc-golf-starter-set.jpg', 65, 0, 'Multicolor'),
        ('Nesting Camp Cookset', 59.00, 4, 'A pot, bowl and utensils that nest into one compact bundle.', 'nesting-camp-cookset.jpg', 45, 0, 'Gray'),
        ('Pop-Up Beach Shade', 129.00, 4, 'A sun shelter that pops up in seconds with UPF 50+ fabric.', 'pop-up-beach-shade.jpg', 30, 0, 'Green'),
        ('Waterproof Picnic Blanket', 39.00, 4, 'A soft blanket with a waterproof back that folds into its own strap.', 'waterproof-picnic-blanket.jpg', 70, 0, 'Plaid'),
        ('Retro Roller Skates', 129.00, 4, 'High-top quad skates with smooth wheels for rinks and boardwalks.', 'retro-roller-skates.jpg', 25, 1, 'White'),
        ('Pickleball Paddle Set', 79.00, 4, 'Two graphite paddles, four balls and a carry bag to start playing today.', 'pickleball-paddle-set.jpg', 60, 0, 'Checkered'),
        ('Trail Running Vest', 119.00, 4, 'A snug hydration vest with soft flasks and room for a jacket.', 'trail-running-vest.jpg', 35, 0, 'Black'),
        ('Stargazing Telescope', 249.00, 4, 'A 90 mm telescope with a phone adapter for the moon, planets and beyond.', 'stargazing-telescope.jpg', 15, 0, 'White'),
        ('Graphite Tennis Racket', 179.00, 4, 'A light, powerful frame with a generous sweet spot.', 'graphite-tennis-racket.jpg', 30, 0, 'Black'),
        ('Camp Hatchet', 45.00, 4, 'A forged steel head and shock-absorbing handle for splitting kindling.', 'camp-hatchet.jpg', 50, 0, 'Black'),
        ('Family Cabin Tent', 299.00, 4, 'A 6-person tent with near-vertical walls, a room divider and space to stand.', 'family-cabin-tent.jpg', 0, 0, 'Green');

-- new drop: fitness & wellness
INSERT INTO products (name, price, category_id, description, image_url, stock, featured, color)
VALUES  ('Smart Body Scale', 59.00, 5, 'Tracks weight, body fat and muscle and syncs to your fitness app.', 'smart-body-scale.jpg', 80, 0, 'Black'),
        ('Foldable Pilates Reformer', 899.00, 5, 'A studio-style reformer that folds flat and rolls under the bed.', 'foldable-pilates-reformer.jpg', 7, 1, 'Natural'),
        ('Adjustable Weight Bench', 199.00, 5, 'Seven back positions from flat to incline for every press and row.', 'adjustable-weight-bench.jpg', 30, 0, 'Black'),
        ('Yoga Block and Strap Set', 25.00, 5, 'Two cork blocks and a cotton strap to deepen every stretch.', 'yoga-block-and-strap-set.jpg', 120, 0, 'Cork'),
        ('Acupressure Mat', 39.00, 5, 'A mat covered in soft spikes that massages your back and feet.', 'acupressure-mat.jpg', 60, 0, 'Teal'),
        ('Cedar Sauna Bucket Set', 69.00, 5, 'A cedar bucket, ladle and thermometer for the perfect sauna session.', 'cedar-sauna-bucket-set.jpg', 40, 0, 'Cedar'),
        ('Slam Medicine Ball', 49.00, 5, 'A grippy, no-bounce ball for slams, throws and core work.', 'slam-medicine-ball.jpg', 55, 0, 'Black'),
        ('Pilates Ring', 19.00, 5, 'A light, flexible ring that adds resistance to Pilates and toning moves.', 'pilates-ring.jpg', 110, 0, 'Purple'),
        ('Ab Roller Wheel', 25.00, 5, 'A wide, stable double wheel with a knee pad for tough core workouts.', 'ab-roller-wheel.jpg', 95, 0, 'Black'),
        ('Wooden Gymnastics Rings', 35.00, 5, 'Birch rings with adjustable straps for pull-ups, dips and muscle-ups.', 'wooden-gymnastics-rings.jpg', 50, 0, 'Natural'),
        ('Speed Agility Ladder', 25.00, 5, 'A 20-foot ladder for quick feet, footwork and warm-up drills.', 'speed-agility-ladder.jpg', 70, 0, 'Yellow'),
        ('Fitness Tracker Band', 79.00, 5, 'A slim band that tracks heart rate, sleep and steps for a week on one charge.', 'fitness-tracker-band.jpg', 65, 0, 'Black'),
        ('Sonic Electric Toothbrush', 89.00, 5, 'Gentle sonic cleaning, a 2-minute timer and a travel case.', 'sonic-electric-toothbrush.jpg', 90, 0, 'Black'),
        ('Sunrise Alarm Clock', 69.00, 5, 'Wakes you gently with a 30-minute glow like a sunrise, plus soft nature sounds.', 'sunrise-alarm-clock.jpg', 55, 0, 'White'),
        ('Weighted Blanket 15 lb', 119.00, 5, 'Evenly weighted glass beads for a calm, hug-like feeling at night.', 'weighted-blanket-15-lb.jpg', 40, 0, 'Gray'),
        ('Olympic Barbell Set', 349.00, 5, 'A 7 ft bar with 230 lb of bumper plates and spring collars.', 'olympic-barbell-set.jpg', 10, 0, 'Black'),
        ('Power Squat Rack', 499.00, 5, 'A steel rack with safety bars and a pull-up bar for heavy home lifts.', 'power-squat-rack.jpg', 12, 1, 'Black'),
        ('Wooden Balance Board', 69.00, 5, 'A rocker board that builds ankle strength, balance and core control.', 'wooden-balance-board.jpg', 45, 0, 'Natural'),
        ('Hot Stone Massage Set', 39.00, 5, 'Smooth basalt stones that hold heat for a soothing massage at home.', 'hot-stone-massage-set.jpg', 60, 0, 'Black'),
        ('Wrist and Ankle Weights', 29.00, 5, 'Adjustable 2.5 lb weights for wrists or ankles that add burn to walks and workouts.', 'wrist-and-ankle-weights.jpg', 100, 0, 'Pink');

-- add shopping cart items
INSERT INTO shopping_cart (user_id, product_id, quantity)
VALUES  (3, 8, 1),
        (3, 10, 1);
