package com.example.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

object DatabaseSeeder {
    private const val TAG = "DatabaseSeeder"

    // Categories supporting user request and existing dataset
    private val categories = listOf(
        mapOf(
            "id" to "apparel",
            "nameAr" to "الأزياء والملابس",
            "nameEn" to "Fashion",
            "iconName" to "checkroom",
            "imageUrl" to "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=600"
        ),
        mapOf(
            "id" to "furniture",
            "nameAr" to "الأثاث والديكور المنزلي",
            "nameEn" to "Home & Furniture",
            "iconName" to "chair",
            "imageUrl" to "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=600"
        ),
        mapOf(
            "id" to "wellness",
            "nameAr" to "العناية والصحة والجمال",
            "nameEn" to "Wellness",
            "iconName" to "spa",
            "imageUrl" to "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=600"
        ),
        mapOf(
            "id" to "artisanal",
            "nameAr" to "الحرف والمنتجات اليدوية",
            "nameEn" to "Artisanal",
            "iconName" to "brush",
            "imageUrl" to "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600"
        ),
        mapOf(
            "id" to "bespoke",
            "nameAr" to "الطلبات والموديلات الخاصة",
            "nameEn" to "Bespoke",
            "iconName" to "palette",
            "imageUrl" to "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=600"
        ),
        mapOf(
            "id" to "electronics",
            "nameAr" to "الإلكترونيات والتقنية",
            "nameEn" to "Electronics",
            "iconName" to "devices",
            "imageUrl" to "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=600"
        ),
        mapOf(
            "id" to "vehicles",
            "nameAr" to "السيارات والمركبات",
            "nameEn" to "Vehicles",
            "iconName" to "directions_car",
            "imageUrl" to "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=600"
        ),
        mapOf(
            "id" to "sports",
            "nameAr" to "الرياضة واللياقة البدنية",
            "nameEn" to "Sports",
            "iconName" to "sports_soccer",
            "imageUrl" to "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=600"
        ),
        mapOf(
            "id" to "services",
            "nameAr" to "الخدمات المهنية والتقنية",
            "nameEn" to "Services",
            "iconName" to "build",
            "imageUrl" to "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600"
        )
    )

    // Seller & Store definitions
    data class SeedProduct(
        val titleEn: String,
        val titleAr: String,
        val descEn: String,
        val descAr: String,
        val price: Double,
        val imageUrl: String,
        val rating: Float,
        val reviews: Int,
        val stock: Int
    )

    data class SeedStore(
        val storeId: String,
        val nameEn: String,
        val nameAr: String,
        val email: String,
        val username: String,
        val password: String = "seeding_password_123", // secure fallback password
        val categoryId: String,
        val logoUrl: String,
        val bannerUrl: String,
        val descEn: String,
        val descAr: String,
        val rating: Float,
        val products: List<SeedProduct>
    )

    private val seedStores = listOf(
        // Store 1: Artisanal - Clay Focus
        SeedStore(
            storeId = "store_aura_artisans",
            nameEn = "Aura Artisans",
            nameAr = "أورا للصناعات اليدوية",
            email = "aura.artisans@market.com",
            username = "aura.artisans",
            password = "seeding_aura_123",
            categoryId = "artisanal",
            logoUrl = "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=1000",
            descEn = "Handcrafted traditional clay pottery and hand-painted ceramic kitchenware with ancient patterns.",
            descAr = "فخار طيني تقليدي مصنوع يدويًا وأواني مطبخ سيراميكية مرسومة يدويًا بنقوش عتيقة زاهية.",
            rating = 4.8f,
            products = listOf(
                SeedProduct(
                    "Embossed Ceramic Fruit Bowl", "وعاء فواكه سيراميك منقوش",
                    "Hand-shaped sand-glazed bowl ideal for fruits and table decor.",
                    "وعاء فواكه مصنوع يدويًا ومطلي بطبقة رملية زجاجية تضفي لمسة عتيقة على طاولتك.",
                    35.0, "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=500", 4.8f, 15, 12
                ),
                SeedProduct(
                    "Clay Flower Vase Classic", "مزهرية فخارية طينية كلاسيكية",
                    "Earthy terracotta tall vase with rustic brushed stripes.",
                    "مزهرية طينية طويلة بلون التيراكوتا الترابي مع خطوط فريدة مرسومة بالفرشاة.",
                    28.0, "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=500", 4.7f, 18, 6
                ),
                SeedProduct(
                    "Glazed Artisanal Coffee Mug", "كوب قهوة يدوي الصنع مطلي",
                    "Speckled oatmeal pattern travel-sized hand-crafted coffee companion.",
                    "كوب قهوة مصنوع يدويًا بنقوش الشوفان المنقطة ليكون رفيقك الدافئ صباحًا.",
                    18.0, "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=500", 4.9f, 32, 20
                ),
                SeedProduct(
                    "Handpainted Tea Set Modern", "طقم شاي عصري مرسوم يدويًا",
                    "A contemporary teapot accompanied by four cups styled in custom teal lacquer.",
                    "إبريق شاي معاصر مصحوب بأربعة أكواب بلون أزرق بحري مطلي يدوياً لتجربة تقديم فريدة.",
                    65.0, "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=500", 4.6f, 9, 5
                ),
                SeedProduct(
                    "Woven Decorative Straw Trivet", "قاعدة أواني من القش المصنوع يدويًا",
                    "Insulative tabletop protector handmade by community weavers.",
                    "واقي طاولة عازل للحرارة منسوج يدويًا بنقوش زاهية من القش الطبيعي.",
                    12.0, "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=500", 4.5f, 25, 40
                ),
                SeedProduct(
                    "Earthy Terracotta Flowerpot", "أصيص نباتات تيراكوتا ترابي",
                    "Breathing clay pot designed to keep home succulents hydrated naturally.",
                    "وعاء فخاري مسامي يسمح للنباتات المنزلية بالتنفس والنمو بشكل صحي.",
                    22.0, "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=500", 4.7f, 11, 15
                ),
                SeedProduct(
                    "Abstract Ceramic Decorative Plate", "طبق سيراميك مزخرف تجريدي",
                    "Individually hand-glazed showcase piece for minimal wall mounts.",
                    "لوحة سيراميكية مزخرفة ومطلية بدقة يدوية لتعليقها أو تزيين الأرفف.",
                    40.0, "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=500", 4.8f, 14, 8
                ),
                SeedProduct(
                    "Artisanal Pottery Water Pitcher", "إبريق ماء فخاري تقليدي",
                    "A tall clay pitcher that naturally cools water in traditional clay fashion.",
                    "إبريق طيني طويل لتبريد وتنقية المياه بشكل طبيعي على الطريقة التقليدية.",
                    48.0, "https://images.unsplash.com/photo-1501370853198-1ad15159074b?w=500", 4.6f, 7, 7
                )
            )
        ),
        // Store 2: Apparel - Minimal Style
        SeedStore(
            storeId = "store_vanguard_apparel",
            nameEn = "Vanguard Apparel",
            nameAr = "طليعة الأزياء والألبسة",
            email = "vanguard.apparel@market.com",
            username = "vanguard.apparel",
            password = "seeding_vanguard_123",
            categoryId = "apparel",
            logoUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1000",
            descEn = "Premium minimalist tailored cotton shirts, organic garments, and everyday wear designed with durable fabrics.",
            descAr = "قمصان قطنية ممتازة وبسيطة التفاصيل، وملابس عضوية مصممة من أقمشة طبيعية متينة يومية.",
            rating = 4.7f,
            products = listOf(
                SeedProduct(
                    "Luxe Tailored Cotton Shirt", "قمصان أوف وايت قطنية فاخرة",
                    "Breathable organic cotton dress shirt featuring double-stitched buttons.",
                    "قميص قطني عضوي مريح ومناسب للاستخدام اليومي والرسمي مع خياطة مزدوجة الأزرار.",
                    55.0, "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=500", 4.7f, 22, 18
                ),
                SeedProduct(
                    "Suede Streetwear Winter Jacket", "سترة شتوية من جلد السويد الكلاسيكي",
                    "Heavyweight, fully-lined camel color suede coat with comfortable cuffs.",
                    "سترة دافئة من جلد السويد (المخمل) باللون الجملي مجهزة ببطانة سميكة مريحة وعملية.",
                    120.0, "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=500", 4.8f, 35, 10
                ),
                SeedProduct(
                    "Relaxed Fit Wool Overcoat", "معطف من الكشمير والصوف المريح",
                    "Cozy, double-breasted charcoal winter jacket with custom horn buttons.",
                    "معطف شتوي طويل مزدوج الصدر بلون رمادي فحمي وأزرار متينة دافئة.",
                    165.0, "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500", 4.9f, 41, 5
                ),
                SeedProduct(
                    "Organic Cotton Crewneck Tee", "قميص قطني كلاسيكي منسوج دافئ",
                    "Pre-washed, ultra-soft everyday crewneck made of sustainable fibers.",
                    "تيشرت كلاسيكي مسبق الغسل مصنوع بالكامل من خيوط القطن العضوية الناعمة.",
                    25.0, "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500", 4.6f, 50, 30
                ),
                SeedProduct(
                    "Modern Denim Utility Pants", "بنطال جينز عملي مريح",
                    "Sturdy indigo wash denim fitted with tailored utility pocket storage.",
                    "بنطال الدنيم القوي والمشبع بلون نيلي مجهز بجيوب جانبية ذكية وخصر مرن.",
                    70.0, "https://images.unsplash.com/photo-1542272604-787c3835535d?w=500", 4.5f, 13, 14
                ),
                SeedProduct(
                    "Classic Wool Knitted Beanie", "قبعة صوفية كلاسيكية دافئة",
                    "Snug, double-layered wool beanie styled in minimalist sand hue.",
                    "قبعة رأس صوفية مزدوجة الطبقات بلون قطني دافئ ومحبوك بشكل ناعم.",
                    18.0, "https://images.unsplash.com/photo-1576871337622-98d48d4aa53e?w=500", 4.8f, 29, 25
                ),
                SeedProduct(
                    "Linen Resort Casual Trouser", "بنطال كتان صيفي خفيف مريح",
                    "An airy, classic-cut linen trouser for seaside or warm weather comfort.",
                    "بنطال مريح وخفيف مصنع من ألياف الكتان الطبيعي لحماية تامة في الأجواء الحارة.",
                    60.0, "https://images.unsplash.com/photo-1509551388413-e18d0ac5d495?w=500", 4.7f, 16, 11
                ),
                SeedProduct(
                    "Premium Canvas Belt Brass", "حزام قماشي بمشبك من النحاس",
                    "Hand-cut canvas strap fitted with solid brass d-rings.",
                    "حزام قماشي متين مصمم بقطع يدوي ومزود بمشبكين متقاطعين من النحاس الخالص.",
                    32.0, "https://images.unsplash.com/photo-1624222247566-7f8240269ac1?w=500", 4.4f, 8, 35
                )
            )
        ),
        // Store 3: Furniture - Oak & Iron
        SeedStore(
            storeId = "store_oak_iron",
            nameEn = "Oak & Iron Furniture",
            nameAr = "بلوط وحديد للأثاث الراق",
            email = "oak.iron@market.com",
            username = "oak.iron",
            password = "seeding_oak_123",
            categoryId = "furniture",
            logoUrl = "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=1000",
            descEn = "Robust hand-welded steel frameworks and solid white oak wood dining tables and industrial furniture.",
            descAr = "هياكل حديدية شديدة التحمل ملحومة يدويًا وطاولات طعام من خشب البلوط الأبيض الصلب وأثاث معاصر مذهل.",
            rating = 4.9f,
            products = listOf(
                SeedProduct(
                    "Rustic Oak Solid Dining Table", "طاولة طعام من خشب البلوط الريفي",
                    "Handmade solid white oak wood table displaying natural live edges.",
                    "طاولة طعام صلبة من خشب البلوط الأبيض الفاخر مع حواف شجر طبيعية لإطلالة دافئة.",
                    750.0, "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=500", 4.9f, 21, 3
                ),
                SeedProduct(
                    "Minimalist Timber Study Desk", "مكتب خشبي كلاسيكي بسيط",
                    "Clean scandinavian design combining white oak drawer with iron frame.",
                    "مكتب دراسي وتطبيقي يدمج خشب البلوط الأبيض مع أرجل حديدية رفيعة سوداء.",
                    380.0, "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=500", 4.8f, 14, 5
                ),
                SeedProduct(
                    "Mid-Century Modern Lounge Chair", "كرسي استرخاء منتصف القرن العصري",
                    "Ergonomically contoured solid walnut armchair upholstered in velvet.",
                    "كرسي وثير مصمم بذراعين من خشب الجوز ومغطى بالمخمل الأخضر الفاخر للاسترخاء.",
                    220.0, "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=500", 4.7f, 33, 8
                ),
                SeedProduct(
                    "Industrial Steel Coffee Table", "طاولة قهوة بنمط حديدي صناعي",
                    "Low-profile geometric table blending micro-plated steel sheets with pine wood.",
                    "طاولة منخفضة الارتفاع تدمج أسطح الخشب الطبيعي مع قاعدة حديدية بنمط هندسي حديث.",
                    180.0, "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=500", 4.6f, 19, 10
                ),
                SeedProduct(
                    "Walnut Modular Floating Bookcase", "رفوف كتب عائمة مفصلة",
                    "Set of three easily mountable timber shelving modules.",
                    "طقم من ثلاثة أرفف خشبية عائمة مصنوعة من خشب الجوز الريفي لتنظيم كتبك.",
                    290.0, "https://images.unsplash.com/photo-1540518614846-7eded433c457?w=500", 4.8f, 12, 7
                ),
                SeedProduct(
                    "Rustic Wood Side Panel Bench", "مقعد خشب ريفي جانبي للمداخل",
                    "Sturdy entryway bench made from kiln-dried solid timber blocks.",
                    "مقعد خشبي مريح وقوي مصنوع من خشب الصنوبر الجاف ليوضع بشكل أنيق في المداخل.",
                    140.0, "https://images.unsplash.com/photo-1581428982868-e410dd047a90?w=500", 4.7f, 26, 12
                ),
                SeedProduct(
                    "Contoured Kitchen Bar Stool", "كرسي بار مطبخ مريح الارتفاع",
                    "Perfect counter-height kitchen stool with matte-black steel legs.",
                    "كرسي بار مرتفع الارتفاع بذراع وظهر داعمين وقاعدة فولاذية متينة مطلية.",
                    95.0, "https://images.unsplash.com/photo-1503602642458-232111445657?w=500", 4.5f, 18, 15
                ),
                SeedProduct(
                    "Live Edge Timber Bench", "مقعد خشب بحواف شجر طبيعية",
                    "Complements the solid oak table, crafted in matching live-edge design.",
                    "مقعد خشبي مميز مكمل لطاولة الطعام الكبيرة ومصنوع من نفس كتلة البلوط الطبيعي وعرقه.",
                    160.0, "https://images.unsplash.com/photo-1595515106969-1ce29566ff1c?w=500", 4.8f, 15, 6
                )
            )
        ),
        // Store 4: Wellness - Nirvana
        SeedStore(
            storeId = "store_nirvana_wellness",
            nameEn = "Nirvana Wellness",
            nameAr = "نيرفانا لمنتجات العناية بالصحة",
            email = "nirvana.wellness@market.com",
            username = "nirvana.wellness",
            password = "seeding_nirvana_123",
            categoryId = "wellness",
            logoUrl = "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=1000",
            descEn = "Holistic certified organic skincare, therapeutic essential oil extracts, and clean apothecary serums designed for mindfulness.",
            descAr = "منتجات العناية بالبشرة العضوية المعتمدة طبيعياً، ومستخلصات الزيوت العطرية العلاجية المحضرة بدقة وعناية.",
            rating = 4.8f,
            products = listOf(
                SeedProduct(
                    "Organic Pure Argan Oil Essence", "مستخلص زيت الأركان العضوي النقي",
                    "100% cold-pressed organic argan oil from sustainable farming.",
                    "زيت أركان نقي ومعصور على البارد بنسبة 100% لتغذية البشرة والشعر وترطيبهما بعمق.",
                    26.0, "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=500", 4.8f, 31, 20
                ),
                SeedProduct(
                    "Rejuvenating Rosehip Night Serum", "سيروم الورد البري لتجديد خلايا البشرة لیلاً",
                    "Infused with therapeutic antioxidants and botanicals for midnight skin glow.",
                    "سيروم ليلي غني بمضادات الأكسدة ومستخلص زهرة الورد البرية لتحسين حيوية الوجه أثناء النوم.",
                    34.0, "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500", 4.9f, 25, 14
                ),
                SeedProduct(
                    "Calming Lavender Essential Oil", "زيت الخزامى (اللافندر) العطري المهدئ الأدبي",
                    "Steam-distilled French lavender extract for aromatherapy.",
                    "زيت لافندر نقي مستخلص بالتقطير بالبخار لتعطير غرفتك وتهدئة الأعصاب والنوم الهادئ.",
                    16.0, "https://images.unsplash.com/photo-1608571424266-edfa99721c3b?w=500", 4.7f, 19, 30
                ),
                SeedProduct(
                    "Relaxing Therapeutic Chamomile Tea", "شاي بالبابونج العلاجي المهدئ للأعصاب",
                    "Loose leaf botanicals of matricaria flower heads to ease stress.",
                    "توليفة من أوراق وأزهار البابونج البري المجففة للاسترخاء وإزالة التوتر قبل النوم.",
                    14.0, "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=500", 4.5f, 12, 50
                ),
                SeedProduct(
                    "Pure Clay Detoxifying Face Mask", "قناع الطين البركاني المنقي للسموم",
                    "Volcanic mineral clay powder that unclogs pores and tones skin naturally.",
                    "بودرة قناع الطين البركاني غنية بالمعادن التي تفتح المسام وتمتص الزهم الزائد بلطف.",
                    22.0, "https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=500", 4.6f, 17, 18
                ),
                SeedProduct(
                    "Antioxidant Green Tea Hydration Spray", "رذاذ مرطب ومغذي بالبشرة بالشاي الأخضر",
                    "Instantly refreshing botanical facial mist for active lifestyles.",
                    "بخاخ مرطب ومنعش للبشرة غني بخلاصة الشاي الأخضر النشط لمكافحة آثار الإجهاد اليومي.",
                    19.0, "https://images.unsplash.com/photo-1616683693504-3ea7e9ad6fec?w=500", 4.8f, 22, 22
                ),
                SeedProduct(
                    "Organic Herbal Lip Care Balm", "مرطب شفاه عشبي مغذي بالنعناع واللافندر",
                    "Scented with peppermint extract and organic shea butter.",
                    "مرطب شفاه ناعم بنكهة النعناع المنعشة والزبدة الطبيعية لشفاه ناعمة على الدوام.",
                    8.0, "https://images.unsplash.com/photo-1617897903246-719242758050?w=500", 4.4f, 40, 100
                ),
                SeedProduct(
                    "Eucalyptus Therapeutic Massage Oil", "زيت تدليك ومساج علاجي بالكينا",
                    "Invigorating aromatherapy base oil infused with refreshing eucalyptol.",
                    "زيت مساج ينبض بالحيوية مستخلص من زيت الكينا والصنوبر واللوز لراحة العضلات المتشنجة.",
                    24.0, "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=500", 4.6f, 15, 25
                )
            )
        ),
        // Store 5: Bespoke - Horology
        SeedStore(
            storeId = "store_bespoke_horology",
            nameEn = "Bespoke Horology",
            nameAr = "بيسبوك لعلوم وتصميم الساعات الفاخرة",
            email = "bespoke.horology@market.com",
            username = "bespoke.horology",
            password = "seeding_bespoke_123",
            categoryId = "bespoke",
            logoUrl = "https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1509048191080-d2984bad6ae5?w=1000",
            descEn = "Individually ordered hand-assembled mechanical wristwatches, customizable dial textures, and personalized engraved cases.",
            descAr = "ساعات يد ميكانيكية مجمعة يدويًا حسب الطلب الفردي، مع واجهات ونقوش مخصصة خصيصًا لاسمك.",
            rating = 4.8f,
            products = listOf(
                SeedProduct(
                    "Bespoke Chronograph Classic Watch", "ساعة كرونوغراف كلاسيكية مصممة حسب الطلب",
                    "Individually customized modern wrist chronograph featuring personal engravings.",
                    "ساعة يد كرونوغراف مخصصة يدوياً، مع إمكانية نقش الحروف الأولى من اسمك على غطائها وهيكلها.",
                    550.0, "https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=500", 4.9f, 8, 4
                ),
                SeedProduct(
                    "Vintage Hand-Winding Mechanical Watch", "ساعة ميكانيكية قديمة تعبأ يدويًا لجامعي الساعات",
                    "Mechanical winding skeleton dial watch with custom leather stitching.",
                    "ساعة يد تاريخية تعود لمنتصف القرن ذات واجهة مكشوفة التروس وحزام جلدي مضلع ويدوي التفصيل.",
                    420.0, "https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?w=500", 4.7f, 12, 3
                ),
                SeedProduct(
                    "Custom Textured Dial Luxury Watch", "ساعة يد فاخرة بواجهة محززة مخصصة لونياً",
                    "Selectable dial plates paired with reliable internal automatic movements.",
                    "ساعة يد آلية الحركة فائقة الدقة والتحمل، مع واجهة خلفية من ألياف الكربون محفورة بديناميكية.",
                    680.0, "https://images.unsplash.com/photo-1509048191080-d2984bad6ae5?w=500", 4.8f, 15, 2
                ),
                SeedProduct(
                    "Skeleton Titanium Custom Watch", "ساعة يد تيتانيوم مكشوفة التروس",
                    "Ultralight titanium housing exposing beautiful intricate gears.",
                    "تحفة تقنية تجمع بين متانة التيتانيوم خفيف الوزن وروعته، مظهرة جمال موازين التروس الآلية الداخلية الساحرة.",
                    980.0, "https://images.unsplash.com/photo-1547996160-81dfa63595aa?w=500", 4.9f, 6, 2
                ),
                SeedProduct(
                    "Engraved Leather Travel Watch Roll", "محفظة ساعات يد من الجلد الطبيعي وحفر مخصص",
                    "Made of genuine leather with individual cushions and monogramming.",
                    "محفظة فاخرة تتسع لثلاث ساعات يد ومصنوعة من جلود طبيعية محفورة لخدمة السفر دون خدش الساعات.",
                    95.0, "https://images.unsplash.com/photo-1588449668365-d15e397f6787?w=500", 4.6f, 11, 10
                ),
                SeedProduct(
                    "Replacement Custom Alligator Strap", "حزام ساعة مخصصة من جلد التمساح",
                    "Hand-constructed luxury replacement band available in various widths.",
                    "حزام ساعة مصنوع يدويًا بالكامل من أفضل طبقات الجلود الطبيعية لإعطاء ساعتك تميزًا عتيقًا متقنًا.",
                    65.0, "https://images.unsplash.com/photo-1624222247566-7f8240269ac1?w=500", 4.5f, 14, 15
                ),
                SeedProduct(
                    "Minimalist Vintage Leather Timepiece", "ساعة يد كلاسيكية بتصميم مائل وذهبي بسيط",
                    "Classic dress watch featuring a clean ivory watch face and customized thin hands.",
                    "انعكاس للبساطة المتناهية مع واجهة عاجية بيضاء ومؤشرات رقيقة منسجمة مع حزام من الجلد العتيق الأسود.",
                    340.0, "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=500", 4.7f, 19, 5
                ),
                SeedProduct(
                    "Custom Winding Timepiece Gold", "ساعة يد يدية التعبئة مطلية بالذهب",
                    "Luxury customized gold plated frame hand-wound mechanical movement.",
                    "إصدار خاص لعشاق الساعات الفخمة باللون الذهبي البراق المنقوش على السيراميك الفولاذي المقاوم للصدأ.",
                    780.0, "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=500", 4.8f, 5, 2
                )
            )
        ),
        // Store 6: Apparel - Thread & Needle
        SeedStore(
            storeId = "store_thread_needle",
            nameEn = "Thread & Needle Haute",
            nameAr = "خيط وإبرة للأزياء الراقية (كوتور)",
            email = "thread.needle@market.com",
            username = "thread.needle",
            password = "seeding_thread_123",
            categoryId = "apparel",
            logoUrl = "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=1000",
            descEn = "Sartorial custom-measured evening wear, evening dresses, tailored suits, and unique silk apparel hand-finished on order.",
            descAr = "حياكة راقية وتصاميم فساتين وبدلات سهرة تخاط خصيصًا بمقاسات عملائنا وبأقمشة حريرية فريدة.",
            rating = 4.8f,
            products = listOf(
                SeedProduct(
                    "Bespoke Heavy Suede Winter Jacket", "سترة شتوية ثقيلة من جلد الغزال الفاخر",
                    "Exquisite warm suede coat featuring premium tailored inner satin.",
                    "سترة دافئة من جلد الغزال والمخمل الفاخر مجهزة ببطانة من الستان والحرير وحياكة يدوية على الأطراف.",
                    195.0, "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=500", 4.9f, 29, 6
                ),
                SeedProduct(
                    "Tailored Italian Wool Suit Jacket", "سترة بدلة من صوف الميرينو الإيطالي الفاخر",
                    "Hand-finished tailored blazer structured in fine merino wool.",
                    "بليزر وبدلة كلاسيكية فاخرة مخيطة يدويًا للكتف والياقة ومبطنة بالكامل بأجود خامات الصوف الإيطالي.",
                    350.0, "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500", 4.8f, 13, 4
                ),
                SeedProduct(
                    "Custom Wool Dress Trousers", "بنطال رسمي من صوف الميرينو الكلاسيكي التابيرد",
                    "Sartorially checked wool formal pants custom hemmed to order.",
                    "بنطال كلاسيكي رسمي بنقش كاروهات رقيق مصنوع ومحبوك يدويًا لضبط القياس وراحة الحركة.",
                    140.0, "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=500", 4.7f, 15, 8
                ),
                SeedProduct(
                    "Fine Mulberry Silk Slip Dress", "فستان حريري ناعم من حرير التوت الطبيعي",
                    "Unmatched luxury draping constructed from pure raw mulberry silk.",
                    "فستان سهرة منسدل بأناقة طبيعية لا تضاهى مصنوع من حرير التوت البري الطبيعي والناعم للغاية.",
                    160.0, "https://images.unsplash.com/photo-1485230895905-ec40ba36b9bc?w=500", 4.8f, 22, 5
                ),
                SeedProduct(
                    "Handcrafted Cashmere Long Sweater", "كنزة طويلة دافئة من كشمير الماعز الخالص",
                    "Incredibly soft knit pullover crafted from pure Mongolian goat hair.",
                    "كنزة شتوية فاخرة ودافئة ذات ملمس فائق النعومة والراحة مصنوع بالكامل من صوف كشمير الماعز المنغولي الخالص.",
                    220.0, "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?w=500", 4.9f, 18, 6
                ),
                SeedProduct(
                    "Italian Casual Tailored Blazer", "معطف بليزر إيطالي كاجوال غير مبطن",
                    "Unstructured shoulder design ideal for smart-casual evening wear.",
                    "بليزر كاجوال بأكتاف مريحة لتضفي مظهرًا أنيقًا واسترخاءً رائعًا لطلعات ومناسبات المساء الراقية.",
                    290.0, "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=500", 4.6f, 11, 7
                ),
                SeedProduct(
                    "Tailored Organic Cotton Linen Shirt", "قميص كتاني قطني طبيعي بتفصيل مريح خفيف",
                    "Crisp, airy tailored dress shirt combining fine cotton with durable flax.",
                    "قميص ناعم ومنعش للغاية يمزج الكتان العضوي والقطن بحرفية تامة ليضمن برودة وراحة كاملة في الصيف.",
                    110.0, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500", 4.7f, 16, 12
                ),
                SeedProduct(
                    "Bespoke Silk Evening Dress", "فستان سهرة حريري رغيد مصمم حسب القياس",
                    "Individually drafted silhouette tailored to personal measurements.",
                    "فستان حفلات وسهرات مصمم بدقة متناهية بمقاسك الخاص المنسجم مع انحسار الحرير ولمعانه الطبيعي الساحر.",
                    420.0, "https://images.unsplash.com/photo-1566174053879-31528523f8ae?w=500", 4.9f, 8, 3
                )
            )
        ),
        // Store 7: Furniture - Atelier Luminara
        SeedStore(
            storeId = "store_atelier_luminara",
            nameEn = "Atelier Luminara",
            nameAr = "أتيليه لومينارا للإضاءات المعاصرة",
            email = "atelier.luminara@market.com",
            username = "atelier.luminara",
            password = "seeding_luminara_123",
            categoryId = "furniture",
            logoUrl = "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?w=1000",
            descEn = "Sculptural hand-blown glass lighting fixtures, floor lamps, and architectural wood decor highlighting warm filaments.",
            descAr = "مصابيح إضاءة منحوتة يدويًا من الزجاج المنفوخ، ومصابيح أرضية، وديكورات مذهلة تبرز الدفء والنقاء العصري.",
            rating = 4.7f,
            products = listOf(
                SeedProduct(
                    "Sculptural Pendant Glass Lamp", "علّاقة إضاءة زجاجية منحوتة فريدة",
                    "Warm hand-blown amber glass pendant hanging on adjustable cable.",
                    "إضاءة سقفية متدلية من الزجاج المنفوخ بلون الكهرمان الدافئ مع كابل تعليق قابل لضبط الارتفاع.",
                    190.0, "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500", 4.8f, 14, 8
                ),
                SeedProduct(
                    "Brass Retro Architectural Floor Lamp", "مصباح أرضي طويل من النحاس العتيق",
                    "Adjustable articulated solid brass stand matching mid-century designs.",
                    "مصباح وإضاءة أرضية ممتدة مع ذراع نحاسي صلب ومفصلي للضبط والتوجيه المناسب لاسترخاء الصالونات.",
                    280.0, "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?w=500", 4.6f, 19, 5
                ),
                SeedProduct(
                    "Modern Minimalist Bedside Light-Bar", "إضاءة سرير حديثة ذكية باللمس",
                    "Sleek aluminum linear light bar offering warm diffused touch sensor adjustments.",
                    "شريط إضاءة خطي ألومنيوم ناعم بقاعدة خشبية يوفر توهجًا وعتمًا مريحًا للسرير عبر اللمس الخفيف.",
                    85.0, "https://images.unsplash.com/photo-1565814636199-ae8133055c1c?w=500", 4.7f, 21, 15
                ),
                SeedProduct(
                    "Architectural Industrial Hanging Chandelier", "ثريا صناعية تعليق خشب وحديد",
                    "Feature layout consisting of five geometric bulbs on timber frames.",
                    "هيكل ثريا مذهلة تضم خمسة مصابيح معلقة على عارضة خشبية سميكة وحبال من القنب بتناسق ترابي رائع.",
                    480.0, "https://images.unsplash.com/photo-1517999144091-3d9dca6d1e43?w=500", 4.9f, 9, 3
                ),
                SeedProduct(
                    "Nordic Beechwood Bedside Table Lamp", "مصباح مائدة من خشب الزان الشمالي",
                    "Minimalist cylinder table lamp displaying organic wood textures.",
                    "أباجورة مائدة كروية دافئة بقاعدة أسطوانية مصنوعة وحاصرة من خشب الزان الطبيعي الرعوي.",
                    65.0, "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=500", 4.5f, 15, 20
                ),
                SeedProduct(
                    "Retro Amber Glass Filament Bulb", "مصابيح إديسون زجاجية كهرمانية عتيقة",
                    "Pack of three classic Edison bulbs providing warm atmospheric light.",
                    "طقم من ثلاثة مصابيح إديسون زجاجية دافئة ذات توهج عتيق ومثالي لصناعة الحميمية والهدوء بالمنزل.",
                    18.0, "https://images.unsplash.com/photo-1550985616-10810253b84d?w=500", 4.4f, 32, 50
                ),
                SeedProduct(
                    "Adjustable Drafting Desk Table Lamp", "مصباح مكتب معدني بذراع متحرك متزن",
                    "Heavy iron base architect-style desktop reading lamp.",
                    "مصباح مكتب بقاعدة ثقيلة وذراع زنبركي حديدي كلاسيكي رائع لخدمة القراءة والمطالعة الهندسية المتزنة.",
                    110.0, "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=500", 4.7f, 16, 12
                ),
                SeedProduct(
                    "Modern Geometric Cube Pendant Light", "مصباح سقف هيروغليفي حديدي مكعب",
                    "Wireframe ceiling hanger focusing on simple, raw geometries.",
                    "مصباح مكعب مفتوح يعلق بالسقف يركز على بساطة الخطوط الهندسية وظلالها الجميلة على الحوائط.",
                    135.0, "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=500", 4.5f, 11, 14
                )
            )
        ),
        // Store 8: Artisanal - Heritage Weavers
        SeedStore(
            storeId = "store_heritage_weavers",
            nameEn = "Heritage Weavers",
            nameAr = "نساجو التراث للسجاد الفاخر",
            email = "heritage.weavers@market.com",
            username = "heritage.weavers",
            password = "seeding_heritage_123",
            categoryId = "artisanal",
            logoUrl = "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1000",
            descEn = "Handmade native geometric rugs, embroidered wool cushions, and natural linen drapes sourced from weaving communities.",
            descAr = "سجاد هندسي منسوج يدويًا محلياً، ووسائد صوفية مطرزة بدقة فائقة من تراث النساجين الأصيل.",
            rating = 4.8f,
            products = listOf(
                SeedProduct(
                    "Turkish Hand-Woven Geometric Rug", "سجاد تركي منسوج يدويًا بنقوش هندسية",
                    "Authentic tribal patterns hand-knotted from organic vegetable-dyed wool.",
                    "سجادة عتيقة من الصوف الطبيعي المصبوغ يدوياً بألوان عشبية هادئة ونقوش قبلية تعكس دفء الشرق.",
                    320.0, "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=500", 4.9f, 19, 4
                ),
                SeedProduct(
                    "Embroidered Berber Tribal Cushion Cover", "غطاء وسادة بربري مطرز يدويات",
                    "Detailed ethnic tassels hand-worked onto heavy linen weave.",
                    "وسادة من الكتان السميك مزخرفة يدويًا وخيوط الصوف الملون بتطريزات أمازيغية أصيلة ناعمة الملمس.",
                    45.0, "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?w=500", 4.8f, 22, 15
                ),
                SeedProduct(
                    "Native Wool Flatweave Bed Runner", "مفرش سرير صوفي مفرود بنقوش هندسية",
                    "Provides cozy warmth, hand-loomed in traditional geometric blocks.",
                    "مفرش دافئ ومزدوج يوضع في نهاية السرير أو الأريكة، منسوج من صوف مغزول بلطف ليبرز الدفء.",
                    115.0, "https://images.unsplash.com/photo-1540518614846-7eded433c457?w=500", 4.6f, 15, 8
                ),
                SeedProduct(
                    "Hand-Loomed Thick Cotton Throw", "غطاء وسادة صوفي وثير محبوك يدوياً",
                    "Comfortable waffle-weave organic cotton cozy blanket.",
                    "شال وسرير وثير مصمم بنقوش هندسية بارزة من خيوط كشمير دافئة وناعمة جداً للمساء الهادئ.",
                    65.0, "https://images.unsplash.com/photo-1600121848594-d8644e57abab?w=500", 4.7f, 11, 20
                ),
                SeedProduct(
                    "Woven Decorative Rattan Floor Pouf", "بوف أرضي من الخيزران المنسوج يدوياً",
                    "Filled with natural fiber stuffing, offering eco-friendly lounging.",
                    "مقعد أرضي متين (بوف) منسوج من قش وقصب الروطان الطبيعي لإضافة إطلالة هادئة للراحة المنزلية.",
                    80.0, "https://images.unsplash.com/photo-1538688525198-9b88f6f53126?w=500", 4.5f, 27, 10
                ),
                SeedProduct(
                    "Mudcloth Style Cotton Table Runner", "مفرش طاولة كتان بنمط طمي أفريقي",
                    "Painted with traditional fermented mud patterns on authentic cotton canvas.",
                    "واجهة ومفرش طاولة طعام من قماش الكتان المغسول والمرسوم يدويًا بمستخلصات طبيعية ريفية هادئة.",
                    38.0, "https://images.unsplash.com/photo-1548690312-e3b507d8c110?w=500", 4.4f, 16, 25
                ),
                SeedProduct(
                    "Coarse Hemp Knitted Cushion Cover", "غطاء وسادة من قماش القنب القوي",
                    "Highly durable natural hemp fiber decor matching rustic settings.",
                    "وسادة متينة للغاية منسوجة وخيوط نبات القنب العتيق والطبيعي، مثالية للجلوس وتزيين الأرائك.",
                    32.0, "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=500", 4.7f, 12, 14
                ),
                SeedProduct(
                    "Native Wool Flatweave Area Rug", "سجادة صوفية كبيرة الحجم محبوكة يدوياً",
                    "Grand masterpiece rug displaying incredible hand-loomed patterns.",
                    "تحفة النساجين الرائعة ذات المساحة الواسعة المنقوشة بفرائد هندسية نادرة وصوف بلدي دافئ مريح للقدمين.",
                    590.0, "https://images.unsplash.com/photo-1540518614846-7eded433c457?w=500", 4.9f, 5, 2
                )
            )
        ),
        // Store 9: Wellness - Apothecary Botanica
        SeedStore(
            storeId = "store_apothecary_botanica",
            nameEn = "Apothecary Botanica",
            nameAr = "بوتانيكا للمستخلصات الطبية والعلاجية",
            email = "apothecary.botanica@market.com",
            username = "apothecary.botanica",
            password = "seeding_apothecary_123",
            categoryId = "wellness",
            logoUrl = "https://images.unsplash.com/photo-1601049541289-9b1b7bbbfe19?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?w=1000",
            descEn = "Micro-batched premium marine collagen, adaptogenic herbal tonics, and organic argan oil therapies for natural inner repair.",
            descAr = "مسحوق كولاجين دقيق مستخلص طبيعيًا، ومستحضرات عشبية تدعم الصحة وتقوي بصيلات الشعر والأظافر.",
            rating = 4.8f,
            products = listOf(
                SeedProduct(
                    "Pure Marine Collagen Peptide Powder", "بودرة ببتيدات الكولاجين البحري النقي",
                    "Hydrolyzed wild-caught marine collagen for glowing hair, skin, and nails.",
                    "بودرة كولاجين بحري متحلل ونقي وسهل الامتصاص لدعم رطوبة وإشراق البشرة وصحة المفاصل.",
                    42.0, "https://images.unsplash.com/photo-1601049541289-9b1b7bbbfe19?w=500", 4.9f, 35, 15
                ),
                SeedProduct(
                    "Natural Premium Argan Oil Hair Serum", "سيروم زيت الأركان الممتاز المطور للشعر",
                    "Micro-batched hair nourishing tonic loaded with Vitamin E for soft locks.",
                    "تركيبة مطورة من زيت الأركان وخلاصة الفيتامينات المغذية لحماية نهايات الشعر من الرطوبة والتلف.",
                    28.0, "https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?w=500", 4.7f, 19, 24
                ),
                SeedProduct(
                    "Organic Rosehip Face Night Glow Lotion", "لوشن مغذي ليلي للبشرة بزيت الورد البري",
                    "Overnight hydration treatment using pure cold-pressed rosehip seed oil extract.",
                    "كريم مرطب غني وعامرة بفوائد الورد البري النقي يعمل على إعادة تنشيط نضارة الوجه المنهك ليلاً.",
                    32.0, "https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?w=500", 4.8f, 22, 18
                ),
                SeedProduct(
                    "Ceremonial Organic Matcha Green Tea", "شاي ماتشا العضوي الاحتفالي الممتاز",
                    "Stone-ground ceremonial grade uji tea loaded with antioxidants.",
                    "مسحوق أوراق الماتشا اليابانية الأصلية الفائقة والمحضرة بدقة لتمنحك طاقة وصفاء ذهن طوال يومك.",
                    38.0, "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=500", 4.6f, 15, 30
                ),
                SeedProduct(
                    "Stress Recovery Adaptogenic Ashwagandha Tonic", "مستخلص الأشواغاندا العضوي المهدئ للأعصاب",
                    "Organic daily drops supporting normal cortisol levels.",
                    "نقاط يومية عشبية مركزة لتقليل التوتر وتعزيز مقاومة الجسم للإرهاق وتحسين النوم بشكل صحي.",
                    29.0, "https://images.unsplash.com/photo-1628135801736-258164ba6bd?w=500", 4.5f, 12, 40
                ),
                SeedProduct(
                    "Organic Cold-Pressed Argan Oil Body balm", "بلسم مرطب للجسم بزيت الأركان العضوي",
                    "Soothing body balm enriched with argan and luxury essential oil.",
                    "كريم ومرهم ترطيب سميك مخصص للجسم شديد الجفاف يحتوي على زيت أركان معصور وشمع عسلي ناعم.",
                    24.0, "https://images.unsplash.com/photo-1590156546746-cf109c3113ef?w=500", 4.7f, 17, 25
                ),
                SeedProduct(
                    "Botanical Rose Water Facial Toner", "تونر ماء الورد الطبيعي المقطر",
                    "Hydrating spray produced by organic steam distillation of Damascus rose petals.",
                    "رذاذ ماء الورد الجوري العضوي المقطر لإنعاش البشرة وتضييق المسام وإضفاء توازن طبيعي ساحر.",
                    18.0, "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?w=500", 4.8f, 29, 35
                ),
                SeedProduct(
                    "Pure Hydrating Marine Collagen Serum", "سيروم الكولاجين البحري المرطب المركز للجفن",
                    "Hyaluronic acid boosted collagen serum for profound skin hydration.",
                    "سيروم فائق النعومة والترطيب يمزج جزيئات الكولاجين وحامض الهيالورونيك لشد البشرة الرقيقة ومقاومة التجاعيد.",
                    36.0, "https://images.unsplash.com/photo-1617897903246-719242758050?w=500", 4.8f, 11, 20
                )
            )
        ),
        // Store 10: Artisanal - Clay Art
        SeedStore(
            storeId = "store_terra_clay",
            nameEn = "Terra & Clay Studios",
            nameAr = "مجموعة تيرا والصلصال لمنتجات الطين المعاصرة",
            email = "terra.clay@market.com",
            username = "terra.clay",
            password = "seeding_terra_123",
            categoryId = "artisanal",
            logoUrl = "https://images.unsplash.com/photo-1565192647048-f997ded87ab7?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=1000",
            descEn = "Hand-thrown sand clay dinnerware, custom rustic mugs, and abstract ceramic sculptures for contemporary spaces.",
            descAr = "أواني طينية مصنوعة بدوران مغزلي، أكواب قهوة ريفية واسعة، ومنحوتات سيراميكية تناسب منازلكم الأنيقة.",
            rating = 4.7f,
            products = listOf(
                SeedProduct(
                    "Speckled Sand Clay Coffee Mug", "كوب قهوة فخاري منقط رملي دافئ",
                    "Chunky organic stoneware mug with a custom hand-pulled handle.",
                    "كوب قهوة ريفي متين مصنوع ومطلي بطبقة ملونة طبيعية تبرز روعة الفخار الحقيقي وتمنح القهوة نكهتها الطبيعية.",
                    18.0, "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=500", 4.8f, 31, 15
                ),
                SeedProduct(
                    "Raw Hand-Thrown Clay Flower Vase", "مزهرية طينية طويلة بنمط تيراكوتا الخام",
                    "Warm unglazed terracotta exterior container ideal for dried florals.",
                    "مزهرية عميقة مفرودة بدون طلاء خارجي لعاشقي التراب والطبيعة، مثالية للأزهار الجافة وأوراق الأوكالبتوس.",
                    32.0, "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=500", 4.7f, 15, 8
                ),
                SeedProduct(
                    "Stoneware Sand Ceramic Dinner Bowl", "وعاء طعام سيراميك رملي مطلي",
                    "Perfect speckled bowl modeled on rustic coastal restaurants.",
                    "وعاء فخاري عريض متعدد الاستخدامات مطلي بنعومة بالداخل وخشن الظهر بمظهر غامق مستوحى من مطاعم الساحل الشهيرة.",
                    25.0, "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=500", 4.8f, 19, 14
                ),
                SeedProduct(
                    "Abstract Clay Desktop Sculpture", "منحوتة تجريدية من الصلصال للمكتب",
                    "Contemporary handmade abstract terracotta figure for decorative sideboards.",
                    "تمثال فني تجريدي منحوت يدويًا من الطين والصلصال، يضفي هيبة وتميزًا لأرفف وصالونات البيت العصري.",
                    75.0, "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=500", 4.6f, 8, 4
                ),
                SeedProduct(
                    "Modern Hanging Clay Planter", "أصيص نباتات فخاري معلق بحبل قنب",
                    "Supplied with high durability hemp hanger rope for indoor ferns.",
                    "أصيص فخاري معلق بحبال قنب قوية مصمم ليتناسب تمامًا مع السرخس والنباتات المنزلية المتدلية.",
                    34.0, "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=500", 4.5f, 14, 10
                ),
                SeedProduct(
                    "Speckled Sand Clay Drinking Cup Set", "طقم أكواب فخارية رملية (٤ حبات)",
                    "Includes four stackable stoneware cups decorated with natural slips.",
                    "مجموعة تضم أربعة أكواب متراصفة فوق بعضها البعض، مصممة بلون التراب والملح الطبيعي لتقدم فيها المشروبات الهادئة.",
                    42.0, "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=500", 4.7f, 22, 12
                ),
                SeedProduct(
                    "Terracotta Mini Desktop Planters", "أصص فخارية صغيرة لصباريات المكتب (٣ حبات)",
                    "Set of three drainage-ready clay pots for miniature desk cactus.",
                    "مجموعة تضم ثلاثة أصص صغيرة مفرزة وعازلة الرطوبة لتزيين طاولة ومكتب العمل بصباريات رقيقة.",
                    18.0, "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=500", 4.4f, 11, 20
                ),
                SeedProduct(
                    "Glazed Ceramic Sand Tapestry Plaque", "لوحة جدارية ممتدة من السيراميك والصلصال",
                    "Handpress decorative clay plate designed for organic styling.",
                    "تحفة سيراميكية بيضاوية ذات نقوش تبرز تدرج حبات الرمل والشمس دمجت لتعليق فخم وهادئ بالبيت.",
                    60.0, "https://images.unsplash.com/photo-1501370853198-1ad15159074b?w=500", 4.6f, 7, 6
                )
            )
        ),
        // Store 11: Bespoke - Signature Tailors
        SeedStore(
            storeId = "store_signature_tailors",
            nameEn = "Signature Tailors",
            nameAr = "التوقيع الخاص للخياطة الكلاسيكية الفاخرة",
            email = "sig.tailors@market.com",
            username = "sig.tailors",
            password = "seeding_signature_123",
            categoryId = "bespoke",
            logoUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1486308512493-ae6a8798ee44?w=1000",
            descEn = "Individually patterned luxury blazers, bespoke silk neckwear, and custom monogrammed leather travel bags.",
            descAr = "معاطف وبدلات رسمية فاخرة تصمم فردياً بكل تفاصيلها وحقائب سفر جلدية منقوشة بالأحرف الأولى من اسمك.",
            rating = 4.9f,
            products = listOf(
                SeedProduct(
                    "Italian Navy Double-Breasted Suit Blazer", "معطف كلاسيكي كحلي دبل-بريستد إيطالي",
                    "Individually structured tailoring made from super-130s Italian worsted wool.",
                    "بليزر كلاسيكي ذو ياقة عريضة وأزرار نحاسية، مصمم يدويًا بمقاس دقيق وأجود خامات الصوف الإيطالي الكحلي.",
                    380.0, "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500", 4.9f, 21, 3
                ),
                SeedProduct(
                    "Bespoke Signature Italian Wool Trousers", "بنطال صوف كتان كلاسيكي مخصص التجهيز",
                    "Expertly measured and tapered luxury pants with personal waist tab adjusters.",
                    "بنطال صوف ميرينو مفرود الارتفاع بلون رمادي وقصة مخروطية ضيقة، مصمم ومحكم التجهيز يدويًا.",
                    150.0, "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=500", 4.8f, 15, 6
                ),
                SeedProduct(
                    "Custom Monogrammed Leather Duffel Bag", "حقيبة يد جلدية فاخرة للسفر بحفر مخصص",
                    "Full-grain vegetable-tanned leather overnight holdall with customized brass tag.",
                    "حقيبة سفر لطلعات نهاية الأسبوع مصنوعة من جلود طبيعية سميكة ومزودة ببطاقة نحاسية لنقش اسمك يدوياً.",
                    240.0, "https://images.unsplash.com/photo-1547949003-9792a18a2601?w=500", 4.9f, 11, 4
                ),
                SeedProduct(
                    "Personalized 18K Gold-Plated Cufflinks", "أزرار كفة قميص ذهبية محفورة مخصصة",
                    "Elegant personalized cufflinks hand-carved with custom block monograms.",
                    "أزرار فاخرة مطلية بالذهب ومرسم عليها الحرفين الأولين لاسمك بحياكة وصياغة يدوية دقيقة تضفي وقاراً تاماً للباس.",
                    120.0, "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=500", 4.8f, 8, 8
                ),
                SeedProduct(
                    "Classic Sartorial Silk Necktie Set", "طقم ربطة عنق ومنديل سهرة حريري فاخر",
                    "Includes customized pure jacquard silk necktie and pocket square.",
                    "طقم كلاسيكي يضم ربطة عنق من الحرير الطبيعي الممتاز ومنديل جيب بنقش الجاكار المحبوك لإطلالة كاملة البهاء.",
                    75.0, "https://images.unsplash.com/photo-1589756823853-eed4a5ad8108?w=500", 4.7f, 16, 20
                ),
                SeedProduct(
                    "Bespoke Monogrammed Slim Wallet", "محفظة بطاقات جلدية رفيعة مدمجة الاسم",
                    "Ultra-thin custom leather wallet engraved with initials.",
                    "حاملة بطاقات رفيعة مصنوعة يدويًا من الجلد الطبيعي ومحفورة الحروف الأولى لتضفي خصوصية وفخامة لجيب المعطف.",
                    65.0, "https://images.unsplash.com/photo-1627123424574-724758594e93?w=500", 4.6f, 32, 25
                ),
                SeedProduct(
                    "Tailored Charcoal Wool Suit Vest", "صديري وريدي شتوي من صوف الفحم الخشن",
                    "Sartorially lined three-button slim wool vest.",
                    "صديري وخصر شتوي مبطن كلاسيكياً بثلاثة أزرار خشبية وصوف خشن دافئ مكمل لبدلة السهرات الدافئة.",
                    115.0, "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=500", 4.7f, 9, 12
                ),
                SeedProduct(
                    "Bespoke Linen Cotton Summer Jacket", "معطف كتان صيفي خفيف مخيط حسب مقاسك",
                    "Custom tailored airy jacket ideal for luxury travel and events.",
                    "بليزر صيفي أنيق وخفيف مستوحى من رحلات السفر البحرية، يمزج الكتان العضوي والحرير لحرية تامة وجاذبية صيفية.",
                    260.0, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500", 4.8f, 12, 5
                )
            )
        ),
        // Store 12: Wellness - Elysian
        SeedStore(
            storeId = "store_elysian_scents",
            nameEn = "Elysian Scents",
            nameAr = "مستخلصات إليسيان للزيوت والعناية",
            email = "elysian.scents@market.com",
            username = "elysian.scents",
            password = "seeding_elysian_123",
            categoryId = "wellness",
            logoUrl = "https://images.unsplash.com/photo-1547887537-6158d64c35b3?w=400",
            bannerUrl = "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=1000",
            descEn = "Sustainably-harvested premium botanical perfumes, custom solid wax colognes, and magnesium body balms.",
            descAr = "عطور طبيعية مستدامة، مستحضرات شمعية مرطبة، ومراهم مغنيسيوم تدعم ارتخاء العضلات والنوم العميق.",
            rating = 4.8f,
            products = listOf(
                SeedProduct(
                    "Pure Organic Rosehip Face Glow Serum", "سيروم الورد البري النقي لإشراق الوجه وعلاجه",
                    "High purity therapeutic face glow serum containing organic rose hip seed oils.",
                    "سيروم مغذي ومجدد لنضارة خلايا البشرة مصنوع بالكامل من زيت زهرة الورد البرية الطبيعية والعلاجية.",
                    35.0, "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500", 4.8f, 15, 10
                ),
                SeedProduct(
                    "Organic Lavender Essential Calm Oil", "زيت اللافندر المهدئ والمساعد على الاسترخاء والهدوء",
                    "Relaxing pure essential oils for bedroom diffusion and therapy.",
                    "زيت لافندر فريد نقي ومركز بالتقطير البخاري الممتاز لتعطير المنزل والمساعدة للنوم العميق والهدوء التام.",
                    16.0, "https://images.unsplash.com/photo-1608571424266-edfa99721c3b?w=500", 4.7f, 32, 20
                ),
                SeedProduct(
                    "Organic Argan Oil Hydrating Face Cream", "كريم مرطب ومنعم للوجه بزيت الأركان المغربي",
                    "Light daily moisturizer containing organic Moroccan argan for intense hydration.",
                    "مرطب يومي خفيف وسهل الامتصاص غني بحمض الهيدروجين وزيت الأركان النقي لترطيب دائم مفعم بالنضارة.",
                    26.0, "https://images.unsplash.com/photo-1590156546746-cf109c3113ef?w=500", 4.6f, 19, 15
                ),
                SeedProduct(
                    "Daily Recovery Magnesium Sleep Balm", "بلسم المغنيسيوم المهدئ للنوم وراحة العضلات",
                    "Applied before resting, enriched with sweet dreams chamomile extracts.",
                    "مرهم طبي مهدئ غني بعنصر المغنيسيوم ومستخلص البابونج البري للمساعدة على استرخاء العضلات ونوم مريح وثير.",
                    22.0, "https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?w=500", 4.8f, 21, 24
                ),
                SeedProduct(
                    "Soothe & Calm Chamomile Daily Mist", "رذاذ البابونج اليومي المنعش للبشرة والوجه",
                    "Hydrating body tonic containing chamomile extracts.",
                    "بخاخ سريع المفعول ومنعش للجسم والوجه بخلاصة زهرة البابونج الطبية المهدئة للحفاظ على رطوبة بشرتك ونعومتها.",
                    18.0, "https://images.unsplash.com/photo-1616683693504-3ea7e9ad6fec?w=500", 4.5f, 16, 30
                ),
                SeedProduct(
                    "Ceremonial Organic Matcha Herbal Tea", "أكياس شاي ماتشا عشبية مهدئة صباحية",
                    "Uji-sourced green tea blends for morning wellness routine.",
                    "مظاريف شاي الماتشا العشبي الأخضر مع أوراق النعناع والشمر المريحة لصحة المعدة ونشاط العقل الصباحي.",
                    20.0, "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=500", 4.7f, 13, 40
                ),
                SeedProduct(
                    "Nourishing Argan Hair Essential Mask", "ماسك وعلاج عميق للشعر بزيت الأركان",
                    "Deep leave-in hair therapy cream containing raw unrefined argan.",
                    "قناع مغذٍ وعلاجي يترك على الشعر لتقوية جذور الشعر الجاف واستعادة لمعانه وصحته بطريقة طبيعية كاملة.",
                    24.0, "https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?w=500", 4.8f, 11, 25
                ),
                SeedProduct(
                    "Damascus Rose Absolute Botanical Perfume", "عطر مطلق بوردة دمشق الجورية الفواح",
                    "Handcrafted luxury perfume produced with natural flower waxes and absolute essence.",
                    "عطر زيتي وشمسي عطري ونادر معبأ يدوياً وبخلاصة شمع وماء الورد السوري الفواح الذي يدوم طويلاً بهيبة ساحرة.",
                    95.0, "https://images.unsplash.com/photo-1547887537-6158d64c35b3?w=500", 4.9f, 6, 8
                )
            )
        )
    )

    private fun getImagesForCategory(categoryId: String, primaryImage: String): List<String> {
        val additional = when (categoryId) {
            "apparel" -> listOf(
                "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=600",
                "https://images.unsplash.com/photo-1509319117193-57bab727e09d?w=600",
                "https://images.unsplash.com/photo-1542272604-787c3835535d?w=600",
                "https://images.unsplash.com/photo-1576871337622-98d48d4aa53e?w=600"
            )
            "furniture" -> listOf(
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600",
                "https://images.unsplash.com/photo-1484101403633-562f891dc89a?w=600",
                "https://images.unsplash.com/photo-1581428982868-e410dd047a90?w=600",
                "https://images.unsplash.com/photo-1503602642458-232111445657?w=600"
            )
            "wellness" -> listOf(
                "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=600",
                "https://images.unsplash.com/photo-1608571424266-edfa99721c3b?w=600",
                "https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=600",
                "https://images.unsplash.com/photo-1616683693504-3ea7e9ad6fec?w=600"
            )
            "artisanal" -> listOf(
                "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=600",
                "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=600",
                "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=600",
                "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=600"
            )
            "bespoke" -> listOf(
                "https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?w=600",
                "https://images.unsplash.com/photo-1547996160-81dfa63595aa?w=600",
                "https://images.unsplash.com/photo-1588449668365-d15e397f6787?w=600",
                "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=600"
            )
            "electronics" -> listOf(
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
                "https://images.unsplash.com/photo-1525609004556-c46c7d6cf0a3?w=600",
                "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=600",
                "https://images.unsplash.com/photo-1496181130207-f397653a0262?w=600"
            )
            "vehicles" -> listOf(
                "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=600",
                "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=600",
                "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=600",
                "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?w=600"
            )
            "sports" -> listOf(
                "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600",
                "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=600",
                "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=600",
                "https://images.unsplash.com/photo-1507398941214-572c25f4b1dc?w=600"
            )
            "services" -> listOf(
                "https://images.unsplash.com/photo-1521791136368-1a46909745f4?w=600",
                "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600",
                "https://images.unsplash.com/photo-1552664730-d307ca884978?w=600",
                "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=600"
            )
            else -> listOf(
                "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=600",
                "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600",
                "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=600"
            )
        }
        val count = (0..3).random()
        val picked = additional.shuffled().take(count)
        return (listOf(primaryImage) + picked).distinct()
    }

    private val seedDirectAds = listOf(
        mapOf(
            "ownerUid" to "ad_user_1",
            "ownerUsername" to "أحمد العتيبي",
            "userAvatar" to "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            "title" to "هاتف آيفون 14 برو ماكس مستعمل بحالة الوكالة 256 جيجا",
            "price" to 850.0,
            "categoryId" to "electronics",
            "condition" to "used",
            "description" to "هاتف آيفون 14 برو ماكس نظيف جداً وصحة البطارية 91% ولم يخضع لأي صيانة من قبل. يأتي مع العلبة الأصلية والشاحن السريع الأصلي للبيع بداعي الترقية والتسليم يد بيد في وسط المدينة.",
            "location" to "عمان، الصويفية",
            "coverImage" to "https://images.unsplash.com/photo-1496181130207-f397653a0262?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_2",
            "ownerUsername" to "رائد القاسم",
            "userAvatar" to "https://images.unsplash.com/photo-1599566150163-29194dcaad36?w=150",
            "title" to "شاشة ألعاب منحنية سامسونج أوديسي 32 بوصة 144 هرتز",
            "price" to 290.0,
            "categoryId" to "electronics",
            "condition" to "used",
            "description" to "شاشة ألعاب ممتازة ومريحة للعين بدقة QHD استجابة 1 ملي ثانية. خالية من البكسلات الميتة أو أي عيوب بصرية. البيع بسعر مغري ومناسب جداً للمصممين ومحبي الألعاب الإلكترونية الثقيلة.",
            "location" to "اربد، شارع الجامعة",
            "coverImage" to "https://images.unsplash.com/photo-1547082299-de196ea013d6?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_3",
            "ownerUsername" to "يوسف الصمادي",
            "userAvatar" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            "title" to "جهاز بلايستيشن 5 نسخة الأقراص مع يدين تحكم وإصدارين من الألعاب",
            "price" to 420.0,
            "categoryId" to "electronics",
            "condition" to "used",
            "description" to "جهاز سوني بلاي ستيشن 5 مستخدم لفترة وجيزة جداً ومعه كامل ملحقاته والكرتون الأصلي. نسخة مميزة تدعم الأقراص واللعب بجودة 4K حقيقية بدون تقطيع.",
            "location" to "الزرقاء، حي معصوم",
            "coverImage" to "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_4",
            "ownerUsername" to "أبو فهد الحسام",
            "userAvatar" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
            "title" to "سيارة تويوتا كامري موديل 2018 فل كامل فحص كامل",
            "price" to 16500.0,
            "categoryId" to "vehicles",
            "condition" to "used",
            "description" to "تويوتا كامري مميزة بلون لؤلؤي خارق النظافة وموفرة جداً في استهلاك الوقود بفضل نظام الهايبرد الهجين. صيانات دورية في الوكالة ولا تحتاج لأي مصاريف استهلاكية حالية.",
            "location" to "عمان، الجبيهة",
            "coverImage" to "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_5",
            "ownerUsername" to "سالم الخالدي",
            "userAvatar" to "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150",
            "title" to "دراجة نارية ياماها R6 موديل 2020 ممشى قليل جداً",
            "price" to 7800.0,
            "categoryId" to "vehicles",
            "condition" to "used",
            "description" to "دراجة ياماها جبارة وسريعة للشباب ومحبي المغامرة والسرعة المنضبطة. إطارات جديدة ممتازة وصيانة شاملة من الزيوت والفلاتر قبل أسبوعين فقط.",
            "location" to "عمان، عبدون",
            "coverImage" to "https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_6",
            "ownerUsername" to "نادر المصري",
            "userAvatar" to "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150",
            "title" to "هوندا سيفيك موديل 2015 بحالة ممتازة واستهلاك رائع",
            "price" to 9500.0,
            "categoryId" to "vehicles",
            "condition" to "used",
            "description" to "سيارة هوندا سيفيك اقتصادية وعائلية ممتازة للاستخدام اليومي والذهاب للجامعة أو العمل. الترخيص والتأمين ساريان لستة أشهر قادمة.",
            "location" to "العقبة، المنطقة الخامسة",
            "coverImage" to "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_7",
            "ownerUsername" to "أم محمد الداود",
            "userAvatar" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            "title" to "طقم كنب تركي فاخر يتسع لـ 9 أشخاص بحالة شبيهة بالجديد",
            "price" to 450.0,
            "categoryId" to "furniture",
            "condition" to "used",
            "description" to "طقم كنب تركي مريح جداً بألوان متناسقة رمادي زيتي وبيج هادئ، يتضمن طاولة وسطية خشبية أنيقة وثلاث طاولات زاوية صغيرة متناسقة. البيع الفوري بداعي السفر العاجل لخارج البلاد.",
            "location" to "عمان، تلاع العلي",
            "coverImage" to "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_8",
            "ownerUsername" to "عمر العبادي",
            "userAvatar" to "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
            "title" to "غرفة نوم ماستر خشب بلوط متين وتصميم ملكي راقٍ",
            "price" to 850.0,
            "categoryId" to "furniture",
            "condition" to "new",
            "description" to "غرفة نوم كاملة تشمل سرير مزدوج كبير وخزانة ملابس بستة أبواب وتسريحة أنيقة مع مرآة مضاءة و2 كومودينة. متينة للغاية وخشب نخب أول لتمنحك الراحة والتميز الملكي.",
            "location" to "اربد، الحي الشرقي",
            "coverImage" to "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_9",
            "ownerUsername" to "فارس الجابري",
            "userAvatar" to "https://images.unsplash.com/photo-1542909168-82c3e7fdca5c?w=150",
            "title" to "طاولة سفرة خشب زان ملونة تتسع لـ 6 مقاعد مخملية مريحة",
            "price" to 220.0,
            "categoryId" to "furniture",
            "condition" to "used",
            "description" to "سفرة طعام دافئة وجميلة تناسب الشقق الحديثة مع مقاعد وثيرة ومخملية ناعمة وسهلة النظيف. بحالة نظيفة جداً وخالية من أي خدوش بصرية وبسعر مغري وقابل للتفاوض.",
            "location" to "عمان، خلدا",
            "coverImage" to "https://images.unsplash.com/photo-1577140917170-285929fb55b7?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_10",
            "ownerUsername" to "خالد منصور",
            "userAvatar" to "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150",
            "title" to "سترة ليزر جلدية إيطالية أصلية لون أسود ملكي",
            "price" to 120.0,
            "categoryId" to "apparel",
            "condition" to "used",
            "description" to "معطف جلدي فاخر قادم ومستورد من إيطاليا مباشرة للرجال مقاس Large. يعطي إطلالة أنيقة ودافئة جداً في الشتاء وفي حالة ممتازة ولا يحتوي على أي تمزقات.",
            "location" to "عمان، الصويفية",
            "coverImage" to "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_11",
            "ownerUsername" to "يسرى كمال",
            "userAvatar" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            "title" to "فستان سهرة وتطريز فلسطيني يدوي أصيل غاية في الروعة",
            "price" to 350.0,
            "categoryId" to "apparel",
            "condition" to "new",
            "description" to "ثوب فلسطيني غرزة فلاحي ناعمة ومحبوك يدوياً على مدى أربعة أشهر. قطعة فنية راقية ومناسبة للمناسبات والأفراح لتجعل حضورك مميزاً للغاية وبألوان تطريز تراثية جذابة جداً.",
            "location" to "عمان، ضاحية الرشيد",
            "coverImage" to "https://images.unsplash.com/photo-1518049360965-5406c5029e2c?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_12",
            "ownerUsername" to "طارق الدباس",
            "userAvatar" to "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
            "title" to "حذاء ركض رياضي ماركة نايكي أصلي مقاس 43 خفيف جداً",
            "price" to 65.0,
            "categoryId" to "apparel",
            "condition" to "used",
            "description" to "حذاء رياضي نخب أول من ناكي اشتريته قبل أسبوع ولم يناسب مقاسي تماماً. لم يلبس سوى مرة واحدة للتجربة ومريح ومثالي للجري الطويل وحماية مفاصل الركبة.",
            "location" to "السلط، وسط البلد",
            "coverImage" to "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_13",
            "ownerUsername" to "عامر التميمي",
            "userAvatar" to "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
            "title" to "مجموعة دمبلز وأثقال حديدية متكاملة 50 كيلو غرام للتدريب المنزلي",
            "price" to 85.0,
            "categoryId" to "sports",
            "condition" to "new",
            "description" to "حقيبة أوزان متكاملة مع ذراع بار طويل للصدر وذراعين صغيرين للدمبلز مع قفازات رياضية مجانية. مناسبة جداً للتمارين الرياضية المنزلية والحفاظ على اللياقة البدنية والنشاط.",
            "location" to "عمان، الياسمين",
            "coverImage" to "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_14",
            "ownerUsername" to "سامر حداد",
            "userAvatar" to "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
            "title" to "دراجة هوائية هجينة تريك أصلية بحالة ممتازة وخفيفة الوزن",
            "price" to 310.0,
            "categoryId" to "sports",
            "condition" to "used",
            "description" to "دراجة تريك رياضية ممتازة للتنزه وممارسة رياضة ركوب الدراجات اليومية. هيكل ألومنيوم خفيف وتروس شيمانو ياباني أصلي مع ملحقات إضاءة وخوذة مجانية.",
            "location" to "مأدبا، حنينا",
            "coverImage" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_15",
            "ownerUsername" to "عادل العساف",
            "userAvatar" to "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150",
            "title" to "طاولة تنس طاولة داخلية مع كامل كرات ومضارب اللعب",
            "price" to 150.0,
            "categoryId" to "sports",
            "condition" to "used",
            "description" to "طاولة تنس طاولة قابلة للطي ومجهزة بعجلات لسهولة المنقل والتخزين. مثالية جداً للاستخدام في غرف المعيشة أو الفناء الخلفي وتأتي مع شبكة قوية وأربع مضارب جودة عالية و6 كرات.",
            "location" to "عمان، مرج الحمام",
            "coverImage" to "https://images.unsplash.com/photo-1534158914592-062992fbe900?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_16",
            "ownerUsername" to "الأستاذ رأفت",
            "userAvatar" to "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150",
            "title" to "خدمة دروس خصوصية رياضيات وفيزياء لطلاب التوجيهي والثانوية",
            "price" to 15.0,
            "categoryId" to "services",
            "condition" to "new",
            "description" to "مدرس ذو خبرة طويلة ومميزة 12 سنة يقدم حصصاً وتدريساً خصوصياً وتبسيطاً للمفاهيم الصعبة للطلاب لضمان التفوق والحصول على أعلى الدرجات. الجلسة الأولى مجانية لتقييم الاحتياج وتحديد خطة الدراسة.",
            "location" to "عمان، الجبيهة",
            "coverImage" to "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_17",
            "ownerUsername" to "مروة عيسى",
            "userAvatar" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            "title" to "خدمة تصميم جرافيكي احترافي شعارات وهوية بصرية للشركات الناشئة",
            "price" to 75.0,
            "categoryId" to "services",
            "condition" to "new",
            "description" to "مصمم جرافيكي مبدع وخبرة بالعمل مع كبرى المؤسسات. أقدم خدمات تصميم الشعارات الفريدة والمطبوعات واللوحات الإعلانية ومحتوى السوشيال ميديا الإبداعي بسرعة وسعر مناسب للجميع.",
            "location" to "الزرقاء، الضليل",
            "coverImage" to "https://images.unsplash.com/photo-1626785774573-4b799315345d?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_18",
            "ownerUsername" to "حمزة قنديل",
            "userAvatar" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            "title" to "برمجة وتطوير تطبيقات موبايل أندرويد وآيفون متكاملة ومحمية",
            "price" to 450.0,
            "categoryId" to "services",
            "condition" to "new",
            "description" to "مطور تطبيقات محترف ذو محفظة أعمال ثرية. أقوم ببناء وتصميم تطبيق متجر إلكتروني أو نظام داخلي لعملك باستخدام أحدث التقنيات مع دعم فني مجاني لمدة ستة أشهر وصيانة دورية شاملة.",
            "location" to "عمان، الشميساني",
            "coverImage" to "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_19",
            "ownerUsername" to "دانية صبحي",
            "userAvatar" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            "title" to "جهاز مساج وتدليك الظهر والأكتاف الكهربائي ذو التدفئة الذكية",
            "price" to 35.0,
            "categoryId" to "wellness",
            "condition" to "new",
            "description" to "جهاز رائع وذكي لإزالة الإرهاق وتشنجات الرقبة والأكتاف بعد ساعات طويلة من العمل المكتبي أو القيادة. يضم 4 بكرات تدوير وتدفئة متناسبة لتنشيط الدورة الدموية للجسم والرقبة.",
            "location" to "عمان، خلدا",
            "coverImage" to "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=600"
        ),
        mapOf(
            "ownerUid" to "ad_user_20",
            "ownerUsername" to "ليلى حداد",
            "userAvatar" to "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=150",
            "title" to "طقم شموع عطرية عضوية طبيعية مهدئة للنوم والاسترخاء",
            "price" to 22.0,
            "categoryId" to "wellness",
            "condition" to "new",
            "description" to "مجموعة مميزة من أربع شموع عطرية مصنوعة بالكامل من شمع الصويا وزيوت اللافندر والياسمين والبابونج والورد الطبيعية. تعطي توهجاً هادئاً وراحة نفسية بالمنزل وساعات احتراق طويلة خالية من الدخان المضر.",
            "location" to "اربد، حوارة",
            "coverImage" to "https://images.unsplash.com/photo-1603006905003-be475563bc59?w=600"
        )
    )

    suspend fun seedReviewsOnly(): Result<Unit> = coroutineScope {
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore is not available", e)
            return@coroutineScope Result.failure(Exception("Firestore is unavailable"))
        }

        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth is not available", e)
            return@coroutineScope Result.failure(Exception("Auth is not available"))
        }

        try {
            Log.d(TAG, "Seeding reviews...")
            
            // Check if reviews already seeded to prevent duplication
            val seedDoc = db.collection("seeds").document("reviews_seeder").get().await()
            if (seedDoc.exists()) {
                Log.d(TAG, "🚀 Reviews already seeded beautifully. Skipping.")
                return@coroutineScope Result.success(Unit)
            }

            // Create or login a dummy user to satisfy Firestore rules
            val dummyEmail = "reviewer_seed@example.com"
            val dummyPassword = "Password123!"
            val uid = try {
                val signupResult = auth.createUserWithEmailAndPassword(dummyEmail, dummyPassword).await()
                signupResult.user?.uid ?: java.util.UUID.randomUUID().toString()
            } catch (e: Exception) {
                try {
                    val loginResult = auth.signInWithEmailAndPassword(dummyEmail, dummyPassword).await()
                    loginResult.user?.uid ?: java.util.UUID.randomUUID().toString()
                } catch (e2: Exception) {
                    java.util.UUID.randomUUID().toString()
                }
            }

            val reviewListParams = listOf(
                Pair(5, "This product is fantastic! Exact quality as described. - منتج رائع جداً، جودة مطابقة للوصف تماماً."),
                Pair(4, "Very good, highly recommended. - جيد جداً، أوصي به بشدة."),
                Pair(5, "Amazing experience, fast shipping. - تجربة مذهلة وشحن سريع."),
                Pair(3, "It's okay, but the packaging was slightly damaged. - لا بأس به، لكن التغليف كان متضرراً قليلاً."),
                Pair(4, "Satisfied with the purchase. - راضٍ تماماً عن الشراء."),
                Pair(5, "Perfect! Beautiful details. - مثالي! تفاصيل جميلة جداً."),
                Pair(1, "Not as expected at all. - ليس كما توقعت أبداً."),
                Pair(2, "Below average quality. - الجودة أقل من المتوسط."),
                Pair(5, "Best value for money. - أفضل قيمة مقابل السعر."),
                Pair(4, "Good quality but shipping took a while. - نوعية جيدة ولكن الشحن استغرق وقتاً طويلاً.")
            )

            val reviewerNames = listOf("Ahmed Ali", "Sarah M", "Omar K", "Mona Y", "Khalid R", "Nour S", "Ali H", "Yousef A")
            val reviewImagesPool = listOf(
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
                "https://images.unsplash.com/photo-1593998066526-65fcab3021a2?w=400",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400"
            )

            var reviewCount = 0
            val allProductDocs = db.collection("products").get().await()
            for (productDoc in allProductDocs.documents) {
                // Generate 2-4 reviews per product
                val numReviews = (2..4).random()
                
                var sumRating = 0
                for (i in 0 until numReviews) {
                    val reviewParam = reviewListParams.random()
                    val reviewerName = reviewerNames.random()
                    val includeImage = java.util.Random().nextBoolean()
                    val images = if (includeImage) listOf(reviewImagesPool.random(), reviewImagesPool.random()) else emptyList()
                    
                    val reviewId = db.collection("products").document(productDoc.id).collection("reviews").document().id
                    val reviewMap = hashMapOf(
                        "id" to reviewId,
                        "productId" to productDoc.id,
                        "userId" to uid,  // Match auth rule!
                        "userName" to reviewerName,
                        "rating" to reviewParam.first,
                        "comment" to reviewParam.second,
                        "images" to images,
                        "createdAt" to System.currentTimeMillis() - (86400000L * (1..30).random())
                    )
                    db.collection("products").document(productDoc.id)
                        .collection("reviews")
                        .document(reviewId)
                        .set(reviewMap)
                        .await()
                    
                    sumRating += reviewParam.first
                    reviewCount++
                }
                
                // Update product stats
                db.collection("products").document(productDoc.id).update(
                    mapOf(
                        "rating" to (sumRating.toDouble() / numReviews),
                        "reviewCount" to numReviews,
                        "totalRatings" to numReviews
                    )
                ).await()
                
                if (reviewCount >= 50) break
            }
            
            // Mark as seeded
            db.collection("seeds").document("reviews_seeder").set(mapOf("timestamp" to System.currentTimeMillis())).await()
            
            // Sign out the dummy user
            auth.signOut()

            Log.d(TAG, "Successfully seeded $reviewCount reviews with images!")
            return@coroutineScope Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding reviews", e)
            return@coroutineScope Result.failure(e)
        }
    }

    suspend fun seedDatabase(): Result<Unit> = coroutineScope {
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore is not available", e)
            return@coroutineScope Result.failure(Exception("Firestore is unavailable: ${e.localizedMessage}"))
        }

        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth is not available", e)
            return@coroutineScope Result.failure(Exception("Auth is unavailable: ${e.localizedMessage}"))
        }

        try {
            // Check if market_v5_verification_seeder seeder is already completed successfully to avoid wasteful execution and rate limits
            val seedDoc = db.collection("seeds").document("market_v5_verification_seeder").get().await()
            if (seedDoc.exists()) {
                Log.d(TAG, "🚀 Database already seeded beautifully with market_v5_verification_seeder dataset. Skipping duplication.")
                return@coroutineScope Result.success(Unit)
            }

            Log.d(TAG, "🧹 Database clean-up started: Deleting old products, stores, categories, direct_ads, interactions...")

            // Helper to wipe collections safely in Firestore
            suspend fun wipeCollection(collectionName: String) {
                val colRef = db.collection(collectionName)
                val snapshot = colRef.get().await()
                if (!snapshot.isEmpty) {
                    val batches = snapshot.documents.chunked(450)
                    for (chunk in batches) {
                        val batch = db.batch()
                        chunk.forEach { doc ->
                            batch.delete(doc.reference)
                        }
                        batch.commit().await()
                    }
                }
                Log.d(TAG, "Wiped collection in Firestore: '$collectionName'")
            }

            // Wipe out existing demo data to ensure a clean relational database
            wipeCollection("products")
            wipeCollection("stores")
            wipeCollection("categories")
            wipeCollection("direct_ads")
            wipeCollection("interactions") // cleanup historic interactions

            // Seed settings/marketplace document
            val settingsData = mapOf(
                "platformFeePercent" to 5.0,
                "vatPercent" to 3.0,
                "defaultShippingFeeSyp" to 20000.0,
                "supportedCities" to listOf("Damascus", "Aleppo", "Homs", "Hama", "Latakia", "Tartous"),
                "supportedPaymentMethods" to listOf("Cash On Delivery", "Syriatel Cash", "MTN Cash"),
                "defaultCurrency" to "USD",
                "defaultExchangeRate" to 13500.0
            )
            db.collection("settings").document("marketplace").set(settingsData).await()
            Log.d(TAG, "Seeded global settings/marketplace rate to 13500.0 SYP/USD")

            // Initialize batch for our primary database seed writes
            val writeBatch = db.batch()

            Log.d(TAG, "🌱 Seeding categories with Fashion & Home & Furniture updates...")
            categories.forEach { cat ->
                val id = cat["id"] as String
                writeBatch.set(db.collection("categories").document(id), cat)
            }

            // 3 additional stores to complete exactly 15 stores target
            val additionalStores = listOf(
                SeedStore(
                    storeId = "store_phoenix_fashion",
                    nameEn = "Phoenix Fashion",
                    nameAr = "أزياء فينيكس",
                    email = "phoenix.fashion@market.com",
                    username = "phoenix.fashion",
                    password = "seeding_phoenix_123",
                    categoryId = "apparel",
                    logoUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=400",
                    bannerUrl = "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1000",
                    descEn = "Premium hand-woven silk fabrics and customized apparel designed for exceptional events.",
                    descAr = "أقمشة حريرية فاخرة مغزولة يدوياً وتصاميم ألبسة فريدة مصممة لتناسب كافة المناسبات.",
                    rating = 4.9f,
                    products = listOf(
                        SeedProduct("Embellished Evening Silk Dress", "فستان سهرة مطرز بالحرير", "Luxurious silk wrap with floral hand embroidery.", "فستان سهرة فاخر بقصة التفاف وتطريزات يدوية من نقوش الورد الجبلي.", 150.0, "https://images.unsplash.com/photo-1566174053879-31528523f8ae?w=500", 4.9f, 15, 5),
                        SeedProduct("Classic Wool Study Blazer", "سترة صوف كلاسيكية للدراسة والعمل", "Hand-tailored tweed structure with deep double ventilation pockets.", "سترة تويد دافئة للعمل مجهزة بجيوب مزدوجة مريحة وحياكة يدوية على الياقة.", 85.0, "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500", 4.7f, 12, 10),
                        SeedProduct("Suede Comfort Loafers", "حذاء لوفر سويدي مرن ومريح", "Flexible split-sole everyday casual shoes.", "حذاء لوفر يومي خفيف مصنوع يدوياً من أفضل طبقات جلد السويد المريح وبلون عسلي.", 65.0, "https://images.unsplash.com/photo-1533867617858-e7b97e060509?w=500", 4.6f, 18, 12),
                        SeedProduct("Silk Pattern Pocket Square", "منديل جيب حريري منقوش", "Finished with rolled edges by local family crafters.", "منديل جيب حريري ناعم منسوج بنقش دمشقي متداخل ليضفي بهاءً وتكاملاً على بذلتك الرسمية.", 15.0, "https://images.unsplash.com/photo-1589756823853-eed4a5ad8108?w=500", 4.5f, 24, 30),
                        SeedProduct("Handmade Leather Holdall Bag", "حقيبة سفر جلدية كبيرة مصنوعة يدوياً", "Robust waxed-thread travel bag featuring solid brass zippers.", "حقيبة عطلات نهاية الأسبوع تتسع لكافة احتياجاتك ومصنوعة من جلود البقر السميكة ومثبتة بنحاس خالص.", 190.0, "https://images.unsplash.com/photo-1588449668365-d15e397f6787?w=500", 4.8f, 9, 3)
                    )
                ),
                SeedStore(
                    storeId = "store_lux_electronics",
                    nameEn = "Lux Electronics",
                    nameAr = "لوكس للإلكترونيات الفاخرة",
                    email = "lux.electronics@market.com",
                    username = "lux.electronics",
                    password = "seeding_lux_123",
                    categoryId = "electronics",
                    logoUrl = "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=400",
                    bannerUrl = "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=1000",
                    descEn = "Bespoke sound setups, custom audiophile walnut headphones, and modern retro music players.",
                    descAr = "تجهيزات صوتية فاخرة وسماعات خشب الجوز الاحترافية بالإضافة لأجهزة تشغيل الموسيقى الكلاسيكية.",
                    rating = 4.8f,
                    products = listOf(
                        SeedProduct("Walnut Wood Pro Audiophile Headphones", "سماعات رأس احترافية من خشب الجوز", "Open-back detailed dynamic response high impedance.", "سماعات رأس مفتوحة الظهر مصنعة بقرص خشبي من الجوز الصلب لصوت دافئ واستجابة ترددية خارقة.", 320.0, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500", 4.9f, 21, 4),
                        SeedProduct("Retro Amber Tube Amp Pre-amplifier", "مضخم صوت كلاسيكي بالأنابيب الكهرمانية", "Warm analog design delivering high-fidelity desktop sound.", "مضخم صوت عتيق بالأنابيب الكهرمانية يضفي دفئاً ومثالية دقيقة لأصوات الآلات الموسيقية لسطح مكتبك.", 180.0, "https://images.unsplash.com/photo-1547082299-de196ea013d6?w=500", 4.7f, 12, 5),
                        SeedProduct("Gold Plated Hi-Fi Interconnects", "كابلات توصيل صوتية مطلية بالذهب", "Shielded oxygen-free copper cables featuring brass housing.", "كابلات فائقة التوصيل والحماية مطلية بالذهب لضمان نقاء الصوت وتألق الترددات وخلوها من الضوضاء.", 45.0, "https://images.unsplash.com/photo-1624222247566-7f8240269ac1?w=500", 4.6f, 35, 15),
                        SeedProduct("Bespoke Walnut Slat Soundbar", "مكبر صوت عريض بشرائح خشب الجوز", "Clean wireless stereophonic column wrapped in acoustic cloth.", "نظام صوتي لاسلكي مدمج مكسو بشرائح من خشب الجوز والشاش الصوتي لتقديم تجربة استماع محيطية متطورة.", 280.0, "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=500", 4.8f, 15, 6),
                        SeedProduct("Minimal Aluminum Desktop DAC", "محول إشارات صوتية رقمي ألومنيوم", "Supports high resolution audio with customizable filter profiles.", "محول صوتي ناصع ومتين يحول الملفات الرقمية لموجات تناظرية فائقة الدقة والوضوح لتفاصيل غنية للغاية.", 110.0, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500", 4.5f, 14, 8)
                    )
                ),
                SeedStore(
                    storeId = "store_velo_sports",
                    nameEn = "Velo Sports Studio",
                    nameAr = "فيلو للرياضة واللياقة",
                    email = "velo.sports@market.com",
                    username = "velo.sports",
                    password = "seeding_velo_123",
                    categoryId = "sports",
                    logoUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400",
                    bannerUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=1000",
                    descEn = "Bespoke high-performance carbon bicycles and custom tailored fitness wear designed for extreme endurance.",
                    descAr = "دراجات هوائية رياضية فائقة الخفة من ألياف الكربون وتجهيزات لياقة مخصصة ومطرزة لقوات التحمل.",
                    rating = 4.7f,
                    products = listOf(
                        SeedProduct("Bespoke Road Carbon Bicycle V1", "دراجة طريق كربونية فائقة الخفة", "Handmade custom paint layout with electronic groupset.", "دراجة طريق يدوية الهيكل من خشب كربون الطيران متميزة بناقل حركة إلكتروني سلس وتصميم انسيابي فريد.", 2400.0, "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=500", 4.9f, 8, 2),
                        SeedProduct("Custom Tailored Cycling Jersey", "قميص ركوب الدراجات الرياضي المخصص", "Ergonomic antibacterial compression pattern woven to fit perfectly.", "قميص رياضي مضاد للتعرق والبكتيريا والروائح يتم ضبط قياساته وخفة دروزه ليجعلك تتنفس بكفاءة في السباقات.", 55.0, "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500", 4.8f, 19, 15),
                        SeedProduct("Pro Carbon Bicycle Helmet", "خوذة دراجين كربونية احترافية", "High safety impact absorption layout featuring magnetic buckles.", "خوذة رأس كربونية بالغة القوة والأمان تدمج تهوية عميقة وتثبيتاً مغناطيسياً سهلاً لخوض المنحدرات بأمان.", 120.0, "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=500", 4.7f, 14, 8),
                        SeedProduct("Custom Compression Sport Socks", "جوارب ضغط رياضية معززة الكاحل", "Reinforced heel cushion structures avoiding blisters.", "جوارب رياضية تعزز الكورة الدموية وتدعم الكاحل لتجنب الإجهاد والاحتكاكات الطويلة أثناء التمرين الصارم.", 18.0, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500", 4.6f, 32, 25),
                        SeedProduct("Suede Padded Road Cycling Gloves", "قفازات قيادة دراجات مبطنة بالسويد", "Dampens vibrations with anatomical placement inserts.", "قفازات يد خفيفة ذات كف من جلد السويد المبطن خصيصاً لامتصاص اهتزاز المقود الطويل وتحسين التشبث.", 35.0, "https://images.unsplash.com/photo-1505664194779-8beaceb93744?w=500", 4.5f, 11, 20)
                    )
                )
            )

            val allStoresToSeed = seedStores + additionalStores
            val savedSellerAccounts = mutableListOf<Map<String, String>>()

            Log.d(TAG, "👥 Dynamic Seller Registration in Firebase Auth & Auth database synchronization")

            // Register all 15 seller accounts asynchronously in Firebase Auth for amazing speed!
            val authDeferreds = allStoresToSeed.map { seedStore ->
                async {
                    try {
                        val signupResult = auth.createUserWithEmailAndPassword(seedStore.email, seedStore.password).await()
                        val uid = signupResult.user?.uid ?: "uid_${seedStore.storeId}"
                        Log.d(TAG, "Successfully registered seller '${seedStore.nameEn}' as Auth User $uid")
                        uid to seedStore
                    } catch (e: Exception) {
                        try {
                            val loginResult = auth.signInWithEmailAndPassword(seedStore.email, seedStore.password).await()
                            val uid = loginResult.user?.uid ?: "uid_${seedStore.storeId}"
                            Log.d(TAG, "Seller already registered, recovered existing Auth User $uid")
                            uid to seedStore
                        } catch (e2: Exception) {
                            val hardcodedUid = "uid_${seedStore.storeId.replace("[^a-zA-Z0-9]".toRegex(), "")}"
                            Log.d(TAG, "Auth registration and login failed for '${seedStore.nameEn}', using stable fallback ID: $hardcodedUid")
                            hardcodedUid to seedStore
                        }
                    }
                }
            }

            val registeredSellers = authDeferreds.awaitAll()

            // 10 custom rates spanning across realistic values requested (12000, 12500, 13000, 11800, etc.)
            val customRatesList = listOf(
                12000.0,
                12500.0,
                13000.0,
                11800.0,
                12200.0,
                12700.0,
                13200.0,
                11500.0,
                12800.0,
                12400.0
            )

            // Map and write each Seller Profile user, Store, and Products data
            registeredSellers.forEachIndexed { i, (uid, seedStore) ->
                // First 5 stores use Global Rate, other 10 stores use Custom Rate
                val usingGlobal = i < 5
                val rate = if (usingGlobal) {
                    13500.0 // settings default rate
                } else {
                    customRatesList[(i - 5) % customRatesList.size]
                }

                // 1. User Record inside 'users' collection
                val userRef = db.collection("users").document(uid)
                val userMap = hashMapOf(
                    "id" to uid,
                    "email" to seedStore.email,
                    "name" to seedStore.nameEn,
                    "isStoreOwner" to true,
                    "role" to "seller",
                    "joinedAt" to System.currentTimeMillis()
                )
                writeBatch.set(userRef, userMap)

                // 2. Store Record inside 'stores' collection
                val storeRef = db.collection("stores").document(seedStore.storeId)
                val storeMap = hashMapOf(
                    "id" to seedStore.storeId,
                    "storeId" to seedStore.storeId, // backward compatibility
                    "name" to seedStore.nameAr,       // Arabic is main localized property
                    "storeName" to seedStore.nameEn,  // English fallback or search
                    "ownerId" to uid,
                    "ownerUserId" to uid,             // backward compatibility
                    "ownerUsername" to seedStore.username,
                    "logoUrl" to seedStore.logoUrl,
                    "bannerUrl" to seedStore.bannerUrl,
                    "description" to seedStore.descAr, // Localized description
                    "categoryId" to seedStore.categoryId,
                    "followersCount" to (100..450).random(),
                    "status" to "active",
                    "rating" to seedStore.rating,
                    "isVerified" to true,
                    "usdExchangeRate" to rate,
                    "createdAt" to System.currentTimeMillis() - (3600000 * (1..48).random()),
                    
                    // Unified exchange rate support
                    "usingGlobalRate" to usingGlobal,
                    "exchangeRate" to rate,
                    "storeCurrency" to "USD",
                    "defaultCurrency" to "USD",
                    "exchangeRateUpdatedAt" to com.google.firebase.Timestamp.now()
                )
                writeBatch.set(storeRef, storeMap)

                // 3. Products under this Store: 5 to 10 products each
                seedStore.products.forEachIndexed { index, item ->
                    val prodId = "prod_${seedStore.storeId}_$index"
                    val prodRef = db.collection("products").document(prodId)

                    val prodImages = getImagesForCategory(seedStore.categoryId, item.imageUrl)
                    val cover = prodImages.firstOrNull() ?: item.imageUrl

                    // Alternating mixed currencies for rich analysis
                    val currency = if (index % 2 == 0) "USD" else "SYP"
                    val priceUSD = item.price
                    val priceSYP = item.price * rate
                    val finalPrice = if (currency == "USD") priceUSD else priceSYP

                    val prodMap = hashMapOf(
                        "id" to prodId,
                        "title" to item.titleAr, // Primary Arabic
                        "name" to item.titleEn,   // Secondary English
                        "description" to item.descAr, // Primary Arabic
                        "descEn" to item.descEn,     // Secondary English
                        "price" to finalPrice,
                        "imageUrls" to prodImages,
                        "images" to prodImages,
                        "coverImage" to cover,
                        "categoryId" to seedStore.categoryId,
                        "category" to seedStore.categoryId,
                        "storeId" to seedStore.storeId,
                        "storeName" to seedStore.nameEn,
                        "storeLogo" to seedStore.logoUrl,
                        "type" to "store_product",
                        "rating" to item.rating,
                        "reviewCount" to item.reviews,
                        "isAvailable" to true,
                        "stock" to item.stock,
                        "stockCount" to item.stock,
                        "createdAt" to System.currentTimeMillis() - (120000 * index),

                        // Dual price conversion fields integration
                        "currency" to currency,
                        "priceUSD" to priceUSD,
                        "priceSYP" to priceSYP,
                        "storeCurrency" to "USD",
                        "exchangeRateUsed" to rate
                    )
                    writeBatch.set(prodRef, prodMap)
                }

                // Append and document credentials for testers so they are saved
                savedSellerAccounts.add(
                    mapOf(
                        "name" to seedStore.nameEn,
                        "email" to seedStore.email,
                        "password" to seedStore.password,
                        "storeId" to seedStore.storeId,
                        "uid" to uid
                    )
                )
            }

            // 4. Seeding Direct Ads (at least 20 items, alternating currencies)
            Log.d(TAG, "🌱 Seeding 20 specified modern direct ads...")
            seedDirectAds.forEachIndexed { index, ad ->
                val adId = "ad_${ad["categoryId"]}_$index"
                val adRef = db.collection("direct_ads").document(adId)

                val adImages = getImagesForCategory(ad["categoryId"] as String, ad["coverImage"] as String)
                val firstImg = adImages.firstOrNull() ?: ad["coverImage"] as String

                val currency = if (index % 2 == 0) "USD" else "SYP"
                val basePrice = ad["price"] as Double
                val price = if (currency == "USD") basePrice else basePrice * 13500.0

                val adMap = hashMapOf(
                    "id" to adId,
                    "adId" to adId,
                    "type" to "direct_ad",
                    "ownerUid" to ad["ownerUid"],
                    "userId" to ad["ownerUid"],
                    "ownerUsername" to ad["ownerUsername"],
                    "userName" to ad["ownerUsername"],
                    "userAvatar" to ad["userAvatar"],
                    "title" to ad["title"],
                    "price" to price,
                    "categoryId" to ad["categoryId"],
                    "condition" to ad["condition"],
                    "description" to ad["description"],
                    "location" to ad["location"],
                    "images" to adImages,
                    "imageUrls" to adImages,
                    "coverImage" to firstImg,
                    "status" to "active",
                    "createdAt" to System.currentTimeMillis() - (3600000 * index),
                    "currency" to currency
                )
                writeBatch.set(adRef, adMap)
            }

            // Write all accounts credentials into a dedicated Firestore seed document for seamless testing
            val seedCredentialsRef = db.collection("seeds").document("seller_accounts")
            writeBatch.set(seedCredentialsRef, mapOf("accounts" to savedSellerAccounts))

            // Write completion metadata flag under market_v5_verification_seeder
            val metaSeedRef = db.collection("seeds").document("market_v5_verification_seeder")
            writeBatch.set(metaSeedRef, mapOf(
                "seededAt" to System.currentTimeMillis(),
                "categoriesCount" to categories.size,
                "storesCount" to allStoresToSeed.size,
                "productsCount" to allStoresToSeed.sumOf { it.products.size },
                "directAdsCount" to seedDirectAds.size,
                "status" to "completed"
            ))

            // Commit the entire relational dataset atomically in a single, efficient, unified batch!
            writeBatch.commit().await()
            Log.d(TAG, "🎉 Firebase Relational Seeding Completed Successfully! Created ${categories.size} categories, ${allStoresToSeed.size} stores, ${allStoresToSeed.sumOf { it.products.size }} products, and ${seedDirectAds.size} direct ads.")

            // Seed reviews sequentially
            Log.d(TAG, "Seeding reviews...")
            val reviewListParams = listOf(
                Pair(5, "This product is fantastic! Exact quality as described. - منتج رائع جداً، جودة مطابقة للوصف تماماً."),
                Pair(4, "Very good, highly recommended. - جيد جداً، أوصي به بشدة."),
                Pair(5, "Amazing experience, fast shipping. - تجربة مذهلة وشحن سريع."),
                Pair(3, "It's okay, but the packaging was slightly damaged. - لا بأس به، لكن التغليف كان متضرراً قليلاً."),
                Pair(4, "Satisfied with the purchase. - راضٍ تماماً عن الشراء."),
                Pair(5, "Perfect! Beautiful details. - مثالي! تفاصيل جميلة جداً."),
                Pair(1, "Not as expected at all. - ليس كما توقعت أبداً."),
                Pair(2, "Below average quality. - الجودة أقل من المتوسط."),
                Pair(5, "Best value for money. - أفضل قيمة مقابل السعر."),
                Pair(4, "Good quality but shipping took a while. - نوعية جيدة ولكن الشحن استغرق وقتاً طويلاً.")
            )

            val reviewerNames = listOf("Ahmed Ali", "Sarah M", "Omar K", "Mona Y", "Khalid R", "Nour S", "Ali H", "Yousef A")
            val reviewImagesPool = listOf(
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
                "https://images.unsplash.com/photo-1593998066526-65fcab3021a2?w=400",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400"
            )

            var reviewCount = 0
            val allProductDocs = db.collection("products").get().await()
            for (productDoc in allProductDocs.documents) {
                // Generate 2-4 reviews per product
                val numReviews = (2..4).random()
                
                var sumRating = 0
                for (i in 0 until numReviews) {
                    val reviewParam = reviewListParams.random()
                    val reviewerName = reviewerNames.random()
                    val includeImage = java.util.Random().nextBoolean()
                    val images = if (includeImage) listOf(reviewImagesPool.random(), reviewImagesPool.random()) else emptyList()
                    
                    val reviewId = db.collection("products").document(productDoc.id).collection("reviews").document().id
                    val reviewMap = hashMapOf(
                        "id" to reviewId,
                        "productId" to productDoc.id,
                        "userId" to "seed_user_${java.util.UUID.randomUUID()}",
                        "userName" to reviewerName,
                        "rating" to reviewParam.first,
                        "comment" to reviewParam.second,
                        "images" to images,
                        "createdAt" to System.currentTimeMillis() - (86400000L * (1..30).random())
                    )
                    db.collection("products").document(productDoc.id)
                        .collection("reviews")
                        .document(reviewId)
                        .set(reviewMap)
                        .await()
                    
                    sumRating += reviewParam.first
                    reviewCount++
                }
                
                // Update product stats
                db.collection("products").document(productDoc.id).update(
                    mapOf(
                        "rating" to (sumRating.toDouble() / numReviews),
                        "reviewCount" to numReviews,
                        "totalRatings" to numReviews
                    )
                ).await()
                
                if (reviewCount >= 50) break
            }
            Log.d(TAG, "Successfully seeded $reviewCount reviews with images!")

            // Log out the last seeder authenticated handler so we don't mess up the application tester session state
            try {
                auth.signOut()
                Log.d(TAG, "Successfully cleared Auth session after seeding.")
            } catch (e: Exception) {
                Log.w(TAG, "Ignore auth signout check", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Seeding failed with fatal exception", e)
            Result.failure(e)
        }
    }
}
