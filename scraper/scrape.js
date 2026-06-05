const puppeteer = require('puppeteer-extra');
const StealthPlugin = require('puppeteer-extra-plugin-stealth');
const axios = require('axios');
const fs = require('fs');
const path = require('path');

puppeteer.use(StealthPlugin());

const RES_DATA_DIR = path.join(__dirname, '..', 'res_data');
const PICS_DIR = path.join(RES_DATA_DIR, 'product_pics');

async function downloadImage(url, filepath) {
    if (!url) return;
    try {
        const response = await axios({
            url,
            method: 'GET',
            responseType: 'stream'
        });
        const writer = fs.createWriteStream(filepath);
        response.data.pipe(writer);
        return new Promise((resolve, reject) => {
            writer.on('finish', resolve);
            writer.on('error', reject);
        });
    } catch (e) {
        console.error('Failed to download image:', url, e.message);
    }
}

function sanitizeFilename(name) {
    if (!name) return 'unknown';
    return name.replace(/[^a-z0-9]/gi, '_').toLowerCase();
}

(async () => {
    console.log('Launching browser...');
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    let vendorData = null;
    let menuPayload = null;
    
    page.on('response', async (response) => {
        const url = response.url();
        // The endpoint usually contains api/v5/vendors/ or similar
        if (url.includes('api/v5/vendors/') && response.request().method() === 'GET') {
            try {
                const json = await response.json();
                console.log('Intercepted vendor API response from:', url);
                fs.writeFileSync('dump.json', JSON.stringify(json, null, 2));
                
                // Foodpanda payload structure often wraps in `data`
                const data = json.data || json;
                if (data && data.menus) {
                    vendorData = data;
                    menuPayload = data.menus;
                }
            } catch (e) {
                // Ignore parsing errors for responses we don't care about
            }
        }
    });

    console.log('Navigating to Foodpanda...');
    await page.goto('https://www.foodpanda.pk/restaurant/r2ki/ice-land-adda-plot', { waitUntil: 'networkidle2', timeout: 60000 });
    
    // Wait an extra few seconds to ensure background XHR complete
    await new Promise(r => setTimeout(r, 5000));

    if (vendorData && menuPayload) {
        console.log('Extracting data...');
        const brandDetails = {
            name: vendorData.name,
            logo: vendorData.hero_image || vendorData.hero_listing_image || vendorData.logo,
            rating: vendorData.rating,
            minimum_order: vendorData.minimum_order_amount,
            delivery_fee: vendorData.delivery_fee_type
        };

        const categories = [];
        const products = [];

        // Usually menus is an array, we take the first active menu
        const menu = menuPayload[0];
        if (menu && menu.menu_categories) {
            for (const category of menu.menu_categories) {
                categories.push({
                    id: category.id,
                    name: category.name
                });
                
                for (const product of category.products) {
                    products.push({
                        id: product.id,
                        category_id: category.id,
                        category_name: category.name,
                        name: product.name,
                        description: product.description,
                        price: product.product_variations?.[0]?.price || 0,
                        image: product.images?.[0]?.image_url || product.file_path || ''
                    });
                }
            }
        }

        const finalData = {
            brand: brandDetails,
            categories,
            products
        };

        fs.writeFileSync(path.join(RES_DATA_DIR, 'restaurant_data.json'), JSON.stringify(finalData, null, 2));
        console.log('Saved restaurant_data.json');

        console.log(`Downloading ${products.length} product images and brand logo...`);
        if (brandDetails.logo) {
            await downloadImage(brandDetails.logo, path.join(PICS_DIR, 'brand_logo.jpg'));
        }

        for (const product of products) {
            if (product.image) {
                const filename = sanitizeFilename(product.name) + '_' + product.id + '.jpg';
                await downloadImage(product.image, path.join(PICS_DIR, filename));
            }
        }
        console.log('All images downloaded successfully.');
    } else {
        console.log('Could not find the menu payload. Please check the intercepted requests or dump.json.');
    }

    await browser.close();
})();
