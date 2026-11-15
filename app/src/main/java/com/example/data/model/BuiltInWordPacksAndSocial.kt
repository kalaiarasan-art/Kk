package com.example.data.model

data class WordEntry(
  val word: String,
  val category: String,
  val difficulty: String, // EASY, MEDIUM, HARD
)

data class StickerDef(
  val id: String,
  val packId: String,
  val label: String,
  val emojiArt: String,
  val badgeText: String,
  val primaryHex: String,
  val accentHex: String,
  val isAnimated: Boolean = true,
)

data class StickerPackDef(
  val id: String,
  val name: String,
  val subtitle: String,
  val unlockRequirement: String,
  val requiredLevel: Int,
  val stickers: List<StickerDef>,
)

data class AchievementDef(
  val id: String,
  val name: String,
  val description: String,
  val iconEmoji: String,
  val xpReward: Int,
  val unlockedTitle: String,
)

object BuiltInCatalog {

  val CATEGORIES = listOf(
    "All Categories",
    "Botanical & Garden",
    "Animals & Critters",
    "Food & Bakery",
    "Cozy Home",
    "Fantasy & Myth",
    "Sports & Action",
    "Music & Art",
    "Space & Weather",
    "Landmarks & Travel",
    "Everyday Tech"
  )

  private val RAW_CATEGORY_WORDS: Map<String, List<Triple<String, String, List<String>>>> = mapOf(
    "Botanical & Garden" to listOf(
      Triple(
        "EASY",
        "Botanical & Garden",
        listOf(
          "sunflower", "cactus", "mushroom", "rose", "tulip", "daisy", "acorn", "pinecone",
          "palm tree", "clover", "pumpkin", "bamboo", "fern", "lily pad", "maple leaf", "cherry blossom",
          "apple tree", "dandelion", "seaweed", "moss", "ivy", "bonsai", "seedling", "watering can",
          "wheelbarrow", "garden hose", "flowerpot", "shovel", "scarecrow", "birdhouse"
        )
      ),
      Triple(
        "MEDIUM",
        "Botanical & Garden",
        listOf(
          "venus flytrap", "weeping willow", "monstera", "lavender", "greenhouse", "honeycomb",
          "terracotta pot", "pruning shears", "stepping stones", "birdbath", "trellis", "compost bin",
          "succulent", "orchid", "Pitcher plant", "artichoke", "pomegranate", "dragonfruit",
          "baobab tree", "water lily", "Wisteria", "gnome", "wind chime", "sundial", "fountain",
          "rain barrel", "gazebo", "hammock", "firefly", "ladybug"
        )
      ),
      Triple(
        "HARD",
        "Botanical & Garden",
        listOf(
          "photosynthesis", "pollination", "botanical garden", "topiary", "hydroponics",
          "terrarium", "chrysanthemum", "rhododendron", "bougainvillea", "eucalyptus",
          "germination", "mycelium", "conservatory", "arboretum", "permaculture",
          "dandelion clock", "tumbleweed", "bioluminescence", "carnivorous plant", "hanging gardens",
          "root system", "treehouse", "hedge maze", "zen garden", "koi pond"
        )
      )
    ),
    "Animals & Critters" to listOf(
      Triple(
        "EASY",
        "Animals & Critters",
        listOf(
          "butterfly", "turtle", "penguin", "giraffe", "elephant", "octopus", "dolphin", "snail",
          "hedgehog", "flamingo", "koala", "panda", "jellyfish", "seahorse", "starfish", "crab",
          "frog", "bee", "owl", "fox", "rabbit", "squirrel", "duck", "swan", "whale", "shark",
          "camel", "zebra", "lion", "monkey"
        )
      ),
      Triple(
        "MEDIUM",
        "Animals & Critters",
        listOf(
          "chameleon", "platypus", "narwhal", "axolotl", "peacock", "porcupine", "armadillo",
          "sloth", "otter", "red panda", "toucan", "hummingbird", "pelican", "pufferfish",
          "mantis", "dragonfly", "caterpillar", "beaver", "raccoon", "meerkat", "kangaroo",
          "walrus", "stingray", "lobster", "hermit crab", "anteater", "lemur", "alpaca", "bison", "moose"
        )
      ),
      Triple(
        "HARD",
        "Animals & Critters",
        listOf(
          "metamorphosis", "hibernation", "echolocation", "camouflage", "migration",
          "komodo dragon", "hammerhead shark", "flying squirrel", "emperor penguin", "snow leopard",
          "praying mantis", "anglerfish", "arctic tern", "chinchilla", "capybara",
          "fiddler crab", "giant squid", "honey badger", "kingfisher", "kiwi bird",
          "manatee", "mudskipper", "okapi", "pangolin", "quokka"
        )
      )
    ),
    "Food & Bakery" to listOf(
      Triple(
        "EASY",
        "Food & Bakery",
        listOf(
          "croissant", "pretzel", "pancake", "waffle", "cupcake", "donut", "pizza", "taco",
          "popcorn", "ice cream", "watermelon", "pineapple", "avocado", "strawberry", "banana",
          "cherries", "grapes", "lemon", "carrot", "broccoli", "corn", "cheese", "sandwich",
          "burger", "hot dog", "sushi", "ramen", "cookie", "pie", "lollipop"
        )
      ),
      Triple(
        "MEDIUM",
        "Food & Bakery",
        listOf(
          "cinnamon roll", "sourdough", "macaron", "gingerbread", "dumpling", "burrito",
          "guacamole", "spaghetti", "lasagna", "fondue", "kebab", "boba tea", "espresso",
          "smoothie", "milkshake", "cotton candy", "fortune cookie", "pineapple pizza",
          "grilled cheese", "french fries", "onion rings", "nachos", "spring roll", "tiramisu",
          "cheesecake", "apple cider", "hot chocolate", "teapot", "french press", "mortar pestle"
        )
      ),
      Triple(
        "HARD",
        "Food & Bakery",
        listOf(
          "charcuterie", "ratatouille", "beef wellington", "souffle", "creme brulee",
          "baked alaska", "profiterole", "pavlova", "churros", "chimichanga",
          "fermentation", "caramelization", "wood fired oven", "chocolate fountain", "picnic basket",
          "farmers market", "spice rack", "sushi conveyor", "tea ceremony", "gingerbread house",
          "birthday cake", "wedding cake", "pepper mill", "waffle iron", "pasta maker"
        )
      )
    ),
    "Cozy Home" to listOf(
      Triple(
        "EASY",
        "Cozy Home",
        listOf(
          "fireplace", "bookshelf", "armchair", "teacup", "candle", "lantern", "umbrella",
          "keyhole", "mirror", "clock", "pillow", "blanket", "slippers", "broom", "bucket",
          "scissors", "needle", "button", "zipper", "toothbrush", "soap", "towel", "bathtub",
          "window", "doorbell", "mailbox", "ladder", "fence", "chimney", "roof"
        )
      ),
      Triple(
        "MEDIUM",
        "Cozy Home",
        listOf(
          "rocking chair", "grandfather clock", "chandelier", "stained glass", "record player",
          "typewriter", "sewing machine", "knitting needles", "yarn ball", "cuckoo clock",
          "hourglass", "snow globe", "music box", "dreamcatcher", "lava lamp", "desk lamp",
          "feather duster", "watering globe", "coat rack", "umbrella stand", "doormat",
          "window box", "spiral staircase", "attic", "balcony", "porch swing", "radiator",
          "ceiling fan", "toaster", "blender"
        )
      ),
      Triple(
        "HARD",
        "Cozy Home",
        listOf(
          "reading nook", "bay window", "secret passage", "revolving door", "dumbwaiter",
          "gramophone", "kaleidoscope", "metronome", "pendulum", "apothecary jar",
          "terrarium jar", "patchwork quilt", "embroidery hoop", "origami crane", "calligraphy pen",
          "wax seal", "magnifying glass", "pocket watch", "oil lantern", "incense burner",
          "tea cozy", "French doors", "skylight", "weathervane", "knocker"
        )
      )
    ),
    "Fantasy & Myth" to listOf(
      Triple(
        "EASY",
        "Fantasy & Myth",
        listOf(
          "dragon", "unicorn", "mermaid", "wizard", "castle", "crown", "sword", "shield",
          "treasure chest", "potion", "magic wand", "crystal ball", "ghost", "vampire",
          "witch hat", "cauldron", "broomstick", "fairy", "goblin", "knight", "pirate",
          "genie lamp", "magic carpet", "compass", "scroll", "torch", "key", "gemstone",
          "monster", "giant"
        )
      ),
      Triple(
        "MEDIUM",
        "Fantasy & Myth",
        listOf(
          "phoenix", "griffin", "pegasus", "centaur", "minotaur", "kraken", "loch ness",
          "yeti", "leprechaun", "medusa", "cyclops", "hydra", "gargoyle", "excalibur",
          "drawbridge", "catapult", "trebuchet", "spellbook", "alchemy", "enchanted forest",
          "floating island", "portal", "dungeon", "labyrinth", "siren", "valkyrie",
          "necromancer", "paladin", "bard", "mimic chest"
        )
      ),
      Triple(
        "HARD",
        "Fantasy & Myth",
        listOf(
          "world tree", "philosopher stone", "fountain of youth", "pandora box", "trojan horse",
          "atlantis", "el dorado", "camelot", "ouroboros", "chimera", "basilisk",
          "cockatrice", "hippogriff", "cerberus", "mjolnir", "poseidon trident", "winged sandals",
          "golden fleece", "rune stone", "astral projection", "time machine", "steampunk airship",
          "clockwork golem", "crystal cavern", "dragon hoard"
        )
      )
    ),
    "Sports & Action" to listOf(
      Triple(
        "EASY",
        "Sports & Action",
        listOf(
          "skateboard", "surfboard", "bicycle", "roller skates", "basketball", "soccer ball",
          "tennis racket", "baseball bat", "bowling pin", "golf club", "hockey stick", "skis",
          "snowboard", "sled", "parachute", "trampoline", "jump rope", "hula hoop", "frisbee",
          "kite", "fishing rod", "canoe", "kayak", "paddle", "medal", "trophy", "whistle",
          "stopwatch", "helmet", "sneakers"
        )
      ),
      Triple(
        "MEDIUM",
        "Sports & Action",
        listOf(
          "archery", "fencing", "badminton", "ping pong", "volleyball", "billiards", "dartboard",
          "boomerang", "unicycle", "pogo stick", "zipline", "rock climbing", "scuba diving",
          "snorkeling", "windsurfing", "water skiing", "ice skating", "curling", "bobsled",
          "Gymnastics", "pommel horse", "high jump", "pole vault", "hurdles", "relay baton",
          "punching bag", "boxing gloves", "karate belt", "dumbbells", "kettlebell"
        )
      ),
      Triple(
        "HARD",
        "Sports & Action",
        listOf(
          "synchronized swimming", "figure skating", "steeplechase", "decathlon", "marathon",
          "peloton", "velodrome", "halfpipe", "kickflip", "slalom", "biathlon",
          "paragliding", "hang gliding", "bungee jumping", "whitewater rafting", "tightrope walker",
          "trapeze artist", "juggling clubs", "obstacle course", "photo finish", "victory podium",
          "penalty kick", "slam dunk", "home run", "checkmate"
        )
      )
    ),
    "Music & Art" to listOf(
      Triple(
        "EASY",
        "Music & Art",
        listOf(
          "guitar", "piano", "drum", "violin", "trumpet", "flute", "harp", "microphone",
          "headphones", "music note", "paintbrush", "palette", "easel", "canvas", "pencil",
          "crayon", "marker", "chalk", "origami", "camera", "theater masks", "ballet shoes",
          "puppet", "statue", "frame", "sketchbook", "eraser", "ruler", "glue", "ribbon"
        )
      ),
      Triple(
        "MEDIUM",
        "Music & Art",
        listOf(
          "saxophone", "accordion", "banjo", "ukulele", "cello", "trombone", "french horn",
          "clarinet", "xylophone", "tambourine", "maracas", "bongo drums", "bagpipes",
          "harmonica", "pan flute", "synthesizer", "turntable", "jukebox", "boombox",
          "pottery wheel", "clay vase", "mosaic", "watercolor", "charcoal sketch", "oil painting",
          "fountain pen", "inkwell", "marble bust", "woodblock print", "screen printing"
        )
      ),
      Triple(
        "HARD",
        "Music & Art",
        listOf(
          "symphony orchestra", "conductor baton", "sheet music", "treble clef", "metronome",
          "theremin", "hurdy gurdy", "didgeridoo", "sitar", "kalimba", "glockenspiel",
          "grand piano", "pipe organ", "string quartet", "amphitheater", "self portrait",
          "still life", "impressionism", "cubism", "surrealism", "chiaroscuro",
          "perspective grid", "pottery kiln", "glassblowing", "ice sculpture"
        )
      )
    ),
    "Space & Weather" to listOf(
      Triple(
        "EASY",
        "Space & Weather",
        listOf(
          "rainbow", "tornado", "lightning", "snowflake", "raindrop", "cloud", "sunshine",
          "crescent moon", "shooting star", "planet", "saturn", "earth", "rocket", "astronaut",
          "spaceship", "alien", "ufo", "telescope", "comet", "meteor", "volcano", "iceberg",
          "mountain", "waterfall", "island", "desert", "canyon", "cave", "wave", "sunset"
        )
      ),
      Triple(
        "MEDIUM",
        "Space & Weather",
        listOf(
          "solar eclipse", "lunar eclipse", "constellation", "milky way", "black hole",
          "space station", "lunar rover", "satellite", "space shuttle", "asteroid belt",
          "northern lights", "aurora", "hurricane", "blizzard", "sandstorm", "whirlpool",
          "geyser", "glacier", "stalactite", "coral reef", "tide pool", "monsoon",
          "barometer", "thermometer", "anemometer", "sundial", "compass rose", "observatory",
          "planetarium", "zero gravity"
        )
      ),
      Triple(
        "HARD",
        "Space & Weather",
        listOf(
          "supernova", "nebula", "event horizon", "wormhole", "exoplanet", "big dipper",
          "orion belt", "space elevator", "terraforming", "solar sail", "james webb",
          "voyager probe", "golden record", "cumulonimbus", "ball lightning", "double rainbow",
          "waterspout", "dust devil", "permafrost", "continental drift", "tectonic plates",
          "magma chamber", "hydrothermal vent", "mirage", "equinox"
        )
      )
    ),
    "Landmarks & Travel" to listOf(
      Triple(
        "EASY",
        "Landmarks & Travel",
        listOf(
          "lighthouse", "windmill", "pyramid", "igloo", "tent", "campfire", "backpack",
          "suitcase", "passport", "map", "binoculars", "train", "airplane", "helicopter",
          "hot air balloon", "sailboat", "submarine", "cruise ship", "bus", "taxi",
          "tractor", "firetruck", "ambulance", "bridge", "tunnel", "statue of liberty",
          "eiffel tower", "castle", "fountain", "ferris wheel"
        )
      ),
      Triple(
        "MEDIUM",
        "Landmarks & Travel",
        listOf(
          "roller coaster", "carousel", "cable car", "gondola", "rickshaw", "tuk tuk",
          "double decker", "steam locomotive", "bullet train", "seaplane", "zeppelin",
          "hovercraft", "icebreaker", "covered bridge", "suspension bridge", "aqueduct",
          "colosseum", "leaning tower", "big ben", "taj mahal", "great wall", "mount rushmore",
          "stonehenge", "easter island", "pagoda", "torii gate", "totem pole", "obelisk",
          "amphitheater", "water wheel"
        )
      ),
      Triple(
        "HARD",
        "Landmarks & Travel",
        listOf(
          "sydney opera house", "golden gate bridge", "machu picchu", "chichen itza",
          "petra", "parthenon", "angkor wat", "neuschwanstein", "mont saint michel",
          "panama canal", "trans siberian", "orient express", "route 66", "silk road",
          "base camp", "caravan", "oasis", "fjord", "archipelago", "atoll",
          "peninsula", "isthmus", "delta", "plateau", "harbor"
        )
      )
    ),
    "Everyday Tech" to listOf(
      Triple(
        "EASY",
        "Everyday Tech",
        listOf(
          "smartphone", "laptop", "keyboard", "mouse", "monitor", "printer", "battery",
          "lightbulb", "magnet", "gear", "wrench", "hammer", "screwdriver", "saw",
          "drill", "flashlight", "calculator", "television", "remote", "radio",
          "clock", "alarm", "fan", "iron", "vacuum", "washing machine", "fridge",
          "microwave", "oven", "plug"
        )
      ),
      Triple(
        "MEDIUM",
        "Everyday Tech",
        listOf(
          "robot", "drone", "3d printer", "vr headset", "smartwatch", "game controller",
          "arcade cabinet", "pinball machine", "vending machine", "cash register",
          "barcode scanner", "microscope", "stethoscope", "x ray", "periscope",
          "solar panel", "wind turbine", "satellite dish", "radar", "sonar",
          "walkie talkie", "morse code", "floppy disk", "cassette tape", "vinyl record",
          "film reel", "projector", "polaroid camera", "tripod", "megaphone"
        )
      ),
      Triple(
        "HARD",
        "Everyday Tech",
        listOf(
          "circuit board", "microchip", "fiber optics", "quantum computer", "server rack",
          "neural network", "cryptography", "hologram", "bionic arm", "exoskeleton",
          "particle accelerator", "tesla coil", "jacobs ladder", "van de graaff",
          "seismograph", "oscilloscope", "pendulum clock", "steam engine", "printing press",
          "cotton gin", "telegraph", "abacus", "astrolabe", "sextant", "sundial"
        )
      )
    )
  )

  val ALL_WORDS: List<WordEntry> by lazy {
    val base = RAW_CATEGORY_WORDS.values.flatten().flatMap { (diff, cat, list) ->
      list.map { WordEntry(word = it.lowercase(), category = cat, difficulty = diff) }
    }
    base
  }

  fun pickCandidateWords(
    count: Int = 3,
    category: String = "All Categories",
    difficulty: String = "MIXED",
    customWordsCsv: String = "",
    seed: Long = System.nanoTime()
  ): List<String> {
    val customList = customWordsCsv.split(",")
      .map { it.trim().lowercase() }
      .filter { it.length in 2..32 }
    val pool = mutableListOf<String>()
    pool.addAll(customList)

    val filtered = ALL_WORDS.filter { entry ->
      val catMatch = category == "All Categories" || entry.category.equals(category, ignoreCase = true)
      val diffMatch = difficulty == "MIXED" || entry.difficulty.equals(difficulty, ignoreCase = true)
      catMatch && diffMatch
    }.map { it.word }

    pool.addAll(if (filtered.isNotEmpty()) filtered else ALL_WORDS.map { it.word })
    val distinctPool = pool.distinct().shuffled(java.util.Random(seed))
    return distinctPool.take(count.coerceIn(1, 5))
  }

  val STICKER_PACKS: List<StickerPackDef> = listOf(
    StickerPackDef(
      id = "starter",
      name = "Botanical Sprouts",
      subtitle = "Warm earthy flora & studio essentials",
      unlockRequirement = "Unlocked at Start",
      requiredLevel = 1,
      stickers = listOf(
        StickerDef("bot_1", "starter", "Sprout Bravo", "🌱✨", "SPROUT!", "#5F7A61", "#DDA15E"),
        StickerDef("bot_2", "starter", "Blooming Idea", "🌻💡", "GENIUS", "#C86446", "#F4A261"),
        StickerDef("bot_3", "starter", "Cozy Tea", "🍵🌿", "COZY", "#3B6E4C", "#E9C46A"),
        StickerDef("bot_4", "starter", "Mushroom Hype", "🍄🔥", "LIT!", "#C86446", "#E76F51"),
        StickerDef("bot_5", "starter", "Cactus Cool", "🌵😎", "SHARP", "#4E6E58", "#8AB17D"),
        StickerDef("bot_6", "starter", "Autumn Leaf", "🍁🎨", "ARTSY", "#BC6C25", "#DDA15E"),
        StickerDef("bot_7", "starter", "Honey Bee", "🐝🍯", "SWEET", "#D4A373", "#FAEDCD"),
        StickerDef("bot_8", "starter", "Lotus Calm", "🪷🧘", "ZEN", "#6D597A", "#B56576"),
        StickerDef("bot_9", "starter", "Pinecone", "🌲🐿️", "CRISP", "#386641", "#A7C957"),
        StickerDef("bot_10", "starter", "Terracotta", "🏺🌿", "CRAFT", "#C86446", "#DDB892"),
      )
    ),
    StickerPackDef(
      id = "reactions",
      name = "Studio Reactions",
      subtitle = "Expressive turn callouts & close guesses",
      unlockRequirement = "Unlocked at Start",
      requiredLevel = 1,
      stickers = listOf(
        StickerDef("rx_gg", "reactions", "Good Game!", "🎨🏆", "GG!", "#C86446", "#DDA15E"),
        StickerDef("rx_close", "reactions", "So Close!", "🤏🔥", "SO CLOSE", "#E07A5F", "#F2CC8F"),
        StickerDef("rx_oops", "reactions", "Happy Accident", "🖌️💦", "OOPS!", "#5F7A61", "#A3B18A"),
        StickerDef("rx_wow", "reactions", "Masterpiece!", "🖼️🤩", "WOW!", "#3B6E4C", "#E9C46A"),
        StickerDef("rx_fast", "reactions", "Speedy Guess", "⚡🧠", "FAST!", "#D97706", "#FDE68A"),
        StickerDef("rx_huh", "reactions", "What Is That?!", "🧐❓", "HUH?!", "#6B705C", "#CB997E"),
        StickerDef("rx_clap", "reactions", "Standing Ovation", "👏🌟", "BRAVO", "#C86446", "#F4A261"),
        StickerDef("rx_laugh", "reactions", "Doodle Giggle", "😂✏️", "HAHA!", "#588157", "#DAD7CD"),
        StickerDef("rx_sweat", "reactions", "Time Running Out", "⏰😅", "PANIC", "#BC4749", "#F2E8CF"),
        StickerDef("rx_heart", "reactions", "Love the Line", "❤️🖌️", "LOVE IT", "#B56576", "#EAAC8B"),
      )
    ),
    StickerPackDef(
      id = "critters",
      name = "Woodland Critters",
      subtitle = "Playful forest friends cheering you on",
      unlockRequirement = "Unlock at Level 2",
      requiredLevel = 2,
      stickers = listOf(
        StickerDef("cr_fox", "critters", "Clever Fox", "🦊🎩", "CLEVER", "#C86446", "#F4A261"),
        StickerDef("cr_owl", "critters", "Night Owl", "🦉🌙", "WISE", "#4A4E69", "#9A8C98"),
        StickerDef("cr_frog", "critters", "Lily Pad Frog", "🐸👑", "RIBBIT", "#386641", "#A7C957"),
        StickerDef("cr_hedgehog", "critters", "Spiky Sketch", "🦔✏️", "POINTY", "#7F5539", "#DDB892"),
        StickerDef("cr_otter", "critters", "Floating Otter", "🦦🌊", "CHILL", "#457B9D", "#A8DADC"),
        StickerDef("cr_panda", "critters", "Bamboo Snack", "🐼🎍", "SNACK", "#5F7A61", "#E9EDC9"),
        StickerDef("cr_snail", "critters", "Slow & Steady", "🐌🎨", "STEADY", "#B08968", "#E6CCB2"),
        StickerDef("cr_bunny", "critters", "Hop To It", "🐰🥕", "HOP!", "#E07A5F", "#F4F1DE"),
        StickerDef("cr_duck", "critters", "Dapper Duck", "🦆🎀", "QUACK", "#3B6E4C", "#E9C46A"),
        StickerDef("cr_cat", "critters", "Studio Cat", "🐱🐾", "PAWSOME", "#6D597A", "#E56B6F"),
      )
    ),
    StickerPackDef(
      id = "master",
      name = "Atelier Legends",
      subtitle = "Prestige stamps for seasoned sketch artists",
      unlockRequirement = "Unlock at Level 3",
      requiredLevel = 3,
      stickers = listOf(
        StickerDef("ma_mona", "master", "Mona Lisa", "🖼️👑", "ICONIC", "#C86446", "#E9C46A"),
        StickerDef("ma_symmetry", "master", "Mandala Pro", "❄️✨", "SYMMETRY", "#3B6E4C", "#8AB17D"),
        StickerDef("ma_palette", "master", "Golden Palette", "🎨🌟", "MAESTRO", "#B5838D", "#FFB4A2"),
        StickerDef("ma_quill", "master", "Calligraphy Quill", "🪶📜", "CLASSIC", "#7F5539", "#EDE0D4"),
        StickerDef("ma_trophy", "master", "Grand Podium", "🏆🎉", "CHAMP", "#D97706", "#FEF08A"),
        StickerDef("ma_fire", "master", "On Fire Streak", "🔥💯", "STREAK", "#BC4749", "#F4A261"),
        StickerDef("ma_rainbow", "master", "Full Spectrum", "🌈🖌️", "VIBRANT", "#457B9D", "#E9C46A"),
        StickerDef("ma_diamond", "master", "Crisp Vector", "💎📐", "PRECISION", "#2A9D8F", "#E9C46A"),
        StickerDef("ma_star", "master", "Hall of Fame", "🌟🏛️", "LEGEND", "#C86446", "#F4A261"),
        StickerDef("ma_crown", "master", "Sketch Royalty", "👑🌿", "ROYALTY", "#386641", "#DDA15E"),
      )
    )
  )

  val ALL_STICKERS: Map<String, StickerDef> by lazy {
    STICKER_PACKS.flatMap { it.stickers }.associateBy { it.id }
  }

  val ACHIEVEMENTS: List<AchievementDef> = listOf(
    AchievementDef("first_blood", "First Blood", "Be the first player to guess a word in a turn.", "⚡", 100, "Quick Thinker"),
    AchievementDef("mona_lisa", "Mona Lisa", "Have every player guess your drawing in under 20 seconds.", "🖼️", 250, "Grand Illustrator"),
    AchievementDef("speed_demon", "Speed Demon", "Guess a word correctly in under 6 seconds.", "🏎️", 200, "Lightning Eye"),
    AchievementDef("night_owl", "Night Owl", "Complete a match with a 100% guess rate.", "🦉", 150, "Midnight Sage"),
    AchievementDef("streak_master", "Streak Master", "Maintain a streak of 3+ consecutive correct guesses.", "🔥", 180, "Unstoppable"),
    AchievementDef("botanical_sketcher", "Botanical Sketcher", "Draw a word from the Botanical & Garden pack.", "🌿", 120, "Botanical Artist"),
    AchievementDef("symmetry_wizard", "Symmetry Wizard", "Use radial or mirror symmetry in a drawing turn.", "❄️", 150, "Kaleidoscope"),
    AchievementDef("color_maestro", "Color Maestro", "Use 5 or more colors in a single drawing.", "🎨", 140, "Chromatic Poet"),
    AchievementDef("social_butterfly", "Social Butterfly", "Send a sticker and a live reaction during a match.", "🦋", 100, "Warm Host"),
    AchievementDef("host_with_most", "Host With The Most", "Create and finish a customized game room.", "🏠", 130, "Studio Host"),
    AchievementDef("daily_challenger", "Daily Challenger", "Play a round in Daily Botanical Challenge mode.", "📅", 160, "Daily Devotee"),
    AchievementDef("flood_fill_pro", "Flood Fill Pro", "Use the deterministic scanline flood fill bucket.", "🪣", 120, "Bucket Boss"),
    AchievementDef("word_smith", "Word Smith", "Create a custom word pack with at least 5 words.", "📝", 150, "Lexicon Crafter"),
    AchievementDef("podium_regular", "Podium Regular", "Finish in the top 3 on the final match podium.", "🥇", 200, "Podium Champion"),
    AchievementDef("century_club", "Thousand Club", "Score over 1,000 total points in a single match.", "💯", 220, "High Scorer"),
    AchievementDef("sticker_collector", "Sticker Collector", "Reach Level 3 and unlock all 4 sticker packs.", "✨", 300, "Atelier Legend")
  )
}
