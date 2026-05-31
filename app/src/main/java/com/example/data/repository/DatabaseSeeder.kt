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

    // 5 Specific Categories required by the prompt
    private val categories = listOf(
        mapOf(
            "id" to "apparel",
            "nameAr" to "الألبسة والأزياء",
            "nameEn" to "Apparel",
            "iconName" to "checkroom",
            "imageUrl" to "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=600"
        ),
        mapOf(
            "id" to "furniture",
            "nameAr" to "الأثاث والديكور",
            "nameEn" to "Furniture",
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
            // Check if market_v2 seeder is already completed successfully to avoid wasteful execution and rate limits
            val seedDoc = db.collection("seeds").document("market_v2").get().await()
            if (seedDoc.exists()) {
                Log.d(TAG, "🚀 Database already seeded beautifully with market_v2 dataset. Skipping duplication.")
                return@coroutineScope Result.success(Unit)
            }

            Log.d(TAG, "🧹 Database clean-up started: Deleting old products, stores, categories, interactions...")

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
            wipeCollection("interactions") // cleanup historic interactions 

            // Initialize batch for our primary database seed writes
            val writeBatch = db.batch()

            Log.d(TAG, "🌱 Seeding 5 specified modern categories: Apparel, Furniture, Wellness, Artisanal, Bespoke")
            categories.forEach { cat ->
                val id = cat["id"] as String
                writeBatch.set(db.collection("categories").document(id), cat)
            }

            val savedSellerAccounts = mutableListOf<Map<String, String>>()

            Log.d(TAG, "👥 Dynamic Seller Registration in Firebase Auth & Auth database synchronization")

            // Register all 12 seller accounts asynchronously in Firebase Auth for amazing speed!
            val authDeferreds = seedStores.map { seedStore ->
                async {
                    try {
                        // Register dynamically
                        val signupResult = auth.createUserWithEmailAndPassword(seedStore.email, seedStore.password).await()
                        val uid = signupResult.user?.uid ?: "uid_${seedStore.storeId}"
                        Log.d(TAG, "Successfully registered seller '${seedStore.nameEn}' as Auth User $uid")
                        uid to seedStore
                    } catch (e: Exception) {
                        // User might already exists, query UID by signing in
                        try {
                            val loginResult = auth.signInWithEmailAndPassword(seedStore.email, seedStore.password).await()
                            val uid = loginResult.user?.uid ?: "uid_${seedStore.storeId}"
                            Log.d(TAG, "Seller already registered, recovered existing Auth User $uid")
                            uid to seedStore
                        } catch (e2: Exception) {
                            // If login also fails (e.g. password mismatch/rate limit), create a deterministic safe UID
                            val hardcodedUid = "uid_${seedStore.storeId.replace("[^a-zA-Z0-9]".toRegex(), "")}"
                            Log.d(TAG, "Auth registration and login failed for '${seedStore.nameEn}', using stable fallback ID: $hardcodedUid")
                            hardcodedUid to seedStore
                        }
                    }
                }
            }

            val registeredSellers = authDeferreds.awaitAll()

            // Map and write each Seller Profile user, Store, and Products data
            registeredSellers.forEach { (uid, seedStore) ->
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
                    "ownerUsername" to seedStore.username,
                    "logoUrl" to seedStore.logoUrl,
                    "bannerUrl" to seedStore.bannerUrl,
                    "description" to seedStore.descAr, // Localized description
                    "categoryId" to seedStore.categoryId,
                    "followersCount" to (100..450).random(),
                    "status" to "active",
                    "rating" to seedStore.rating,
                    "isVerified" to true,
                    "usdExchangeRate" to 13500.0,
                    "createdAt" to System.currentTimeMillis() - (3600000 * (1..48).random())
                )
                writeBatch.set(storeRef, storeMap)

                // 3. Products under this Store: 8 products each
                seedStore.products.forEachIndexed { index, item ->
                    val prodId = "prod_${seedStore.storeId}_$index"
                    val prodRef = db.collection("products").document(prodId)

                    val prodMap = hashMapOf(
                        "id" to prodId,
                        "title" to item.titleAr, // Primary Arabic 
                        "name" to item.titleEn,   // Secondary English
                        "description" to item.descAr, // Primary Arabic
                        "descEn" to item.descEn,     // Secondary English
                        "price" to item.price,
                        "imageUrls" to listOf(item.imageUrl),
                        "images" to listOf(item.imageUrl),
                        "coverImage" to item.imageUrl,
                        "categoryId" to seedStore.categoryId,
                        "category" to seedStore.categoryId,
                        "storeId" to seedStore.storeId,
                        "rating" to item.rating,
                        "reviewCount" to item.reviews,
                        "isAvailable" to true,
                        "stockCount" to item.stock,
                        "createdAt" to System.currentTimeMillis() - (120000 * index)
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

            // Write all accounts credentials into a dedicated Firestore seed document for seamless testing 
            val seedCredentialsRef = db.collection("seeds").document("seller_accounts")
            writeBatch.set(seedCredentialsRef, mapOf("accounts" to savedSellerAccounts))

            // Write completion metadata flag to ensure seeder is never repeated once successfully processed is done
            val metaSeedRef = db.collection("seeds").document("market_v2")
            writeBatch.set(metaSeedRef, mapOf(
                "seededAt" to System.currentTimeMillis(),
                "categoriesCount" to categories.size,
                "storesCount" to seedStores.size,
                "productsCount" to seedStores.size * 8,
                "status" to "completed"
            ))

            // Commit the entire relational dataset atomically in a single, efficient, unified batch!
            writeBatch.commit().await()
            Log.d(TAG, "🎉 Firebase Relational Seeding Completed Successfully! Created ${categories.size} categories, ${seedStores.size} stores and ${seedStores.size * 8} products.")

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
