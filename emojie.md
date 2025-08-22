Documentation
List all emojis
Endpoint to retrieve a list of all emojis

https://emoji-api.com/emojis?access_key=414ee18c8fec19984dd2aecc72b46e343e2cfb4c
[
    {
        "slug": "grinning-face",
        "character": "\ud83d\ude00",
        "unicodeName": "grinning face",
        "codePoint": "1F600",
        "group": "smileys-emotion",
        "subGroup": "face-smiling"
    },
    {
        "slug": "grinning-face-with-big-eyes",
        "character": "\ud83d\ude03",
        "unicodeName": "grinning face with big eyes",
        "codePoint": "1F603",
        "group": "smileys-emotion",
        "subGroup": "face-smiling"
    },
    {
        "slug": "grinning-face-with-smiling-eyes",
        "character": "\ud83d\ude04",
        "unicodeName": "grinning face with smiling eyes",
        "codePoint": "1F604",
        "group": "smileys-emotion",
        "subGroup": "face-smiling"
    },
    {
        "slug": "beaming-face-with-smiling-eyes",
        "character": "\ud83d\ude01",
        "unicodeName": "beaming face with smiling eyes",
        "codePoint": "1F601",
        "group": "smileys-emotion",
        "subGroup": "face-smiling"
    },
    {
        "slug": "grinning-squinting-face",
        "character": "\ud83d\ude06",
        "unicodeName": "grinning squinting face",
        "codePoint": "1F606",
        "group": "smileys-emotion",
        "subGroup": "face-smiling"
    }
]
Search for emojis
Endpoint to query emojis by a searchstring

https://emoji-api.com/emojis?search=computer&access_key=414ee18c8fec19984dd2aecc72b46e343e2cfb4c
[
    {
        "slug": "laptop-computer",
        "character": "\ud83d\udcbb",
        "unicodeName": "laptop computer",
        "codePoint": "1F4BB",
        "group": "objects",
        "subGroup": "computer"
    },
    {
        "slug": "desktop-computer",
        "character": "\ud83d\udda5\ufe0f",
        "unicodeName": "desktop computer",
        "codePoint": "1F5A5 FE0F",
        "group": "objects",
        "subGroup": "computer"
    },
    {
        "slug": "computer-mouse",
        "character": "\ud83d\uddb1\ufe0f",
        "unicodeName": "computer mouse",
        "codePoint": "1F5B1 FE0F",
        "group": "objects",
        "subGroup": "computer"
    },
    {
        "slug": "computer-disk",
        "character": "\ud83d\udcbd",
        "unicodeName": "computer disk",
        "codePoint": "1F4BD",
        "group": "objects",
        "subGroup": "computer"
    }
]
Get a single emoji
Endpoint to retrieve a single emojis information

https://emoji-api.com/emojis/grinning-squinting-face?access_key=414ee18c8fec19984dd2aecc72b46e343e2cfb4c
[
    {
        "slug": "grinning-squinting-face",
        "character": "\ud83d\ude06",
        "unicodeName": "grinning squinting face",
        "codePoint": "1F606",
        "group": "smileys-emotion",
        "subGroup": "face-smiling"
    }
]
Get categories
Endpoint to retrieve a list of all emoji categories

https://emoji-api.com/categories?access_key=414ee18c8fec19984dd2aecc72b46e343e2cfb4c
[
    {
        "slug": "smileys-emotion",
        "subCategories": [
            "face-smiling",
            "face-affection",
            "face-tongue",
            "face-hand",
            "face-neutral-skeptical",
            "face-sleepy",
            "face-unwell",
            "face-hat",
            "face-glasses",
            "face-concerned",
            "face-negative",
            "face-costume",
            "cat-face",
            "monkey-face",
            "emotion"
        ]
    },
    {
        "slug": "people-body",
        "subCategories": [
            "hand-fingers-open",
            "hand-fingers-partial",
            "hand-single-finger",
            "hand-fingers-closed",
            "hands",
            "hand-prop",
            "body-parts",
            "person",
            "person-gesture",
            "person-role",
            "person-fantasy",
            "person-activity",
            "person-sport",
            "person-resting",
            "family",
            "person-symbol"
        ]
    },
    {
        "slug": "component",
        "subCategories": [
            "skin-tone",
            "hair-style"
        ]
    },
    {
        "slug": "animals-nature",
        "subCategories": [
            "animal-mammal",
            "animal-bird",
            "animal-amphibian",
            "animal-reptile",
            "animal-marine",
            "animal-bug",
            "plant-flower",
            "plant-other"
        ]
    },
    {
        "slug": "food-drink",
        "subCategories": [
            "food-fruit",
            "food-vegetable",
            "food-prepared",
            "food-asian",
            "food-marine",
            "food-sweet",
            "drink",
            "dishware"
        ]
    }
]
Get emojis in a category
Endpoint to retrieve a all emoji by a given category

https://emoji-api.com/categories/travel-places?access_key=414ee18c8fec19984dd2aecc72b46e343e2cfb4c
[
    {
        "slug": "globe-showing-europe-africa",
        "character": "\ud83c\udf0d",
        "unicodeName": "globe showing Europe-Africa",
        "codePoint": "1F30D",
        "group": "travel-places",
        "subGroup": "place-map"
    },
    {
        "slug": "globe-showing-americas",
        "character": "\ud83c\udf0e",
        "unicodeName": "globe showing Americas",
        "codePoint": "1F30E",
        "group": "travel-places",
        "subGroup": "place-map"
    },
    {
        "slug": "globe-showing-asia-australia",
        "character": "\ud83c\udf0f",
        "unicodeName": "globe showing Asia-Australia",
        "codePoint": "1F30F",
        "group": "travel-places",
        "subGroup": "place-map"
    },
    {
        "slug": "globe-with-meridians",
        "character": "\ud83c\udf10",
        "unicodeName": "globe with meridians",
        "codePoint": "1F310",
        "group": "travel-places",
        "subGroup": "place-map"
    },
    {
        "slug": "world-map",
        "character": "\ud83d\uddfa\ufe0f",
        "unicodeName": "world map",
        "codePoint": "1F5FA FE0F",
        "group": "travel-places",
        "subGroup": "place-map"
    }
]
by 220

current unicode version: 15

last 