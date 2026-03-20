package com.meandr.meandrDataServices.osm.model;

/**
 * OSM entity types that Meandr supports — mapped to OSM tag key/value pairs.
 * Each enum value knows its OSM query filter and how to display itself to users.
 */
public enum OsmEntityType {

    // ── Scenic / Viewpoints ──────────────────────────────────────────────────
    SCENIC_OVERLOOK     ("tourism",   "viewpoint",          "Scenic Overlook",      "🌄"),
    COVERED_BRIDGE      ("man_made",  "bridge",             "Covered Bridge",       "🌉"),
    SCENIC_BYWAY        ("route",     "scenic",             "Scenic Byway",         "🛣️"),

    // ── Water Features ───────────────────────────────────────────────────────
    WATERFALL           ("waterway",  "waterfall",          "Waterfall",            "💧"),
    HOT_SPRING          ("natural",   "hot_spring",         "Hot Spring",           "♨️"),
    SWIMMING_HOLE       ("leisure",   "swimming_area",      "Swimming Hole",        "🏊"),
    RIVER_ACCESS        ("leisure",   "fishing",            "River Access",         "🎣"),

    // ── Trails & Hiking ──────────────────────────────────────────────────────
    TRAILHEAD           ("highway",   "trailhead",          "Trailhead",            "🥾"),
    PICNIC_SITE         ("tourism",   "picnic_site",        "Picnic Site",          "🧺"),
    WILDERNESS_HUT      ("tourism",   "wilderness_hut",     "Wilderness Hut",       "🏕️"),

    // ── Climbing ─────────────────────────────────────────────────────────────
    CLIMBING_CRAG       ("sport",     "climbing",           "Climbing Area",        "🧗"),
    BOULDER             ("natural",   "rock",               "Boulder",              "🪨"),

    // ── Nature & Wildlife ────────────────────────────────────────────────────
    NATURE_RESERVE      ("leisure",   "nature_reserve",     "Nature Reserve",       "🌿"),
    BIRD_HIDE           ("leisure",   "bird_hide",          "Bird Hide",            "🦅"),
    CAVE                ("natural",   "cave_entrance",      "Cave",                 "🦇"),
    PEAK                ("natural",   "peak",               "Mountain Peak",        "⛰️"),
    SPRING              ("natural",   "spring",             "Natural Spring",       "💦"),

    // ── Astronomy ────────────────────────────────────────────────────────────
    OBSERVATORY         ("man_made",  "observatory",        "Observatory",          "🔭"),
    DARK_SKY_AREA       ("leisure",   "dark_sky_area",      "Dark Sky Area",        "🌌"),

    // ── Heritage & History ───────────────────────────────────────────────────
    ARCHAEOLOGICAL_SITE ("historic",  "archaeological_site","Archaeological Site",  "🏛️"),
    RUINS               ("historic",  "ruins",              "Ruins",                "🏚️"),
    BATTLEFIELD         ("historic",  "battlefield",        "Battlefield",          "⚔️"),
    WAYSIDE_SHRINE      ("historic",  "wayside_shrine",     "Historic Shrine",      "🪦"),
    MILESTONE           ("historic",  "milestone",          "Historic Milestone",   "🪧"),

    // ── Quirky / Roadside ────────────────────────────────────────────────────
    ARTWORK             ("tourism",   "artwork",            "Public Art",           "🎨"),
    ATTRACTION          ("tourism",   "attraction",         "Roadside Attraction",  "✨"),
    GHOST_TOWN          ("place",     "ghost_town",         "Ghost Town",           "👻");

    public final String osmKey;
    public final String osmValue;
    public final String displayName;
    public final String emoji;

    OsmEntityType(String osmKey, String osmValue, String displayName, String emoji) {
        this.osmKey      = osmKey;
        this.osmValue    = osmValue;
        this.displayName = displayName;
        this.emoji       = emoji;
    }
}