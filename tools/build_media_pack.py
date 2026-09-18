#!/usr/bin/env python3
"""Builds TotPocket's photo pack from hand-sorted photos.

Every photo listed in PHOTOS below is resized to WebP (full size + thumbnail) and described in
catalog.json. Photos sharing a subject are grouped into slideshow "videos" for the pretend
YouTube. Everything is zipped into the app's compose resources:

    shared/src/commonMain/composeResources/files/media/pack.zip
        catalog.json
        images/<category>/<id>.webp   (long edge 1080px)
        thumbs/<category>/<id>.webp   (long edge 360px)

Usage:  python3 tools/build_media_pack.py [SOURCE_DIR]      (default: ~/Downloads; needs Pillow)

Photos come from Unsplash (Unsplash License: free to use, no permission needed) and from Wikimedia
Commons (approved entries in tools/commons_photos.json, fetched by tools/fetch_commons_photos.py;
mostly CC BY / CC BY-SA). Each photo's author, licence and page are recorded in the catalog, and
the app shows the credit under the photo.
"""

import io
import json
import sys
import zipfile
from collections import OrderedDict, defaultdict
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "shared/src/commonMain/composeResources/files/media/pack.zip"
PACK_VERSION = 1
FULL_EDGE, FULL_QUALITY = 1080, 68
THUMB_EDGE, THUMB_QUALITY = 360, 60

CATEGORIES = OrderedDict(
    animals="Animals",
    birds="Birds",
    critters="Bugs & critters",
    food="Food",
    body="My body",
    things="Everyday things",
    vehicles="Vehicles",
    nature="Nature & flowers",
    village="Village life",
)

# subject -> (video title, sound in compose resources or None)
SUBJECTS = {
    "cat": ("Cute cats", "files/gallery/animals/cat.ogg"),
    "dog": ("Doggies", "files/gallery/animals/dog.ogg"),
    "monkey": ("Funny monkeys", None),
    "cow": ("Moo cows", "files/gallery/animals/cow.ogg"),
    "goat": ("Little goats", None),
    "deer": ("Baby deer", None),
    "tiger": ("Big tigers", None),
    "elephant": ("Elephants", "files/gallery/animals/elephant.ogg"),
    "crow": ("Crows", "files/gallery/nature/birds.ogg"),
    "sparrow": ("Sparrows", "files/gallery/nature/birds.ogg"),
    "parrot": ("Green parrots", "files/gallery/nature/birds.ogg"),
    "duck": ("Quack quack ducks", "files/gallery/animals/duck.ogg"),
    "chicken": ("Chickens", "files/gallery/animals/chicken.ogg"),
    "bird": ("Birds", "files/gallery/nature/birds.ogg"),
    "butterfly": ("Butterflies", None),
    "bee": ("Busy bees", "files/gallery/animals/bee.ogg"),
    "dragonfly": ("Dragonflies", None),
    "snail": ("Slow snails", None),
    "frog": ("Frogs", "files/gallery/animals/frog.ogg"),
    "lizard": ("Lizards", None),
    "ant": ("Tiny ants", None),
    "fish": ("Fish", None),
    "car": ("Cars", None),
    "bus": ("Buses", None),
    "train": ("Trains", None),
    "motorbike": ("Motorbikes", None),
    "boat": ("Boats", None),
    "rickshaw": ("Rickshaws", None),
    "truck": ("Trucks", None),
    "people": ("Village life", None),
    "sheep": ("Baa baa sheep", "files/gallery/animals/sheep.ogg"),
    "horse": ("Horses", "files/gallery/animals/horse.ogg"),
    "lion": ("Lions", "files/gallery/animals/lion.ogg"),
    "zebra": ("Stripy zebras", None),
    "giraffe": ("Tall giraffes", None),
    "rabbit": ("Bunny rabbits", None),
    "panda": ("Pandas", None),
    "owl": ("Owls", "files/gallery/nature/birds.ogg"),
    "peacock": ("Peacocks", None),
    "penguin": ("Penguins", None),
    "pigeon": ("Pigeons", "files/gallery/nature/birds.ogg"),
    "turtle": ("Turtles", None),
    "ladybug": ("Ladybugs", None),
    "milk": ("Milk", None),
    "jackfruit": ("Jackfruit", None),
    "mango": ("Mangoes", None),
    "banana": ("Bananas", None),
    "rice": ("Rice for lunch", None),
    "fruits": ("Fruits", None),
    "chocolate": ("Chocolate", None),
    "apple": ("Apples", None),
    "orange": ("Oranges", None),
    "watermelon": ("Watermelon", None),
    "strawberry": ("Strawberries", None),
    "beard": ("Beards", None),
    "smile": ("Smiles", None),
    "eye": ("Eyes", None),
    "water": ("Water", None),
    "toothbrush": ("Brush your teeth", None),
    "cup": ("Cups", None),
    "spoon": ("Spoons", None),
    "plate": ("Plates", None),
    "bowl": ("Bowls", None),
    "ball": ("Balls", None),
    "shoes": ("Shoes", None),
    "chair": ("Chairs", None),
    "clock": ("Tick tock clocks", None),
    "umbrella": ("Umbrellas", None),
    "book": ("Books", None),
    "teddy": ("Teddy bears", None),
    "toycar": ("Toy cars", None),
    "bucket": ("Buckets", None),
    "soap": ("Soap", None),
    "comb": ("Combs", None),
    "key": ("Keys", None),
    "lamp": ("Lamps", None),
    "pencil": ("Colour pencils", None),
    "balloon": ("Balloons", None),
    "hat": ("Hats", None),
    "kettle": ("Kettles", None),
    "door": ("Doors", None),
    "airplane": ("Aeroplanes", None),
    "helicopter": ("Helicopters", None),
    "firetruck": ("Fire engines", None),
    "tractor": ("Tractors", None),
    "bicycle": ("Bicycles", None),
    "rainbow": ("Rainbows", None),
    "moon": ("The moon", None),
    "sunflower": ("Sunflowers", "files/gallery/flowers/sunflower.ogg"),
    "rose": ("Roses", "files/gallery/flowers/rose.ogg"),
    "lotus": ("Lotus flowers", None),
    "waterlily": ("Water lilies", None),
}

# source file -> (category, subject, title). Sorted by looking at every photo; the WhatsApp
# event photos, wallpapers, stock/watermarked photos, food, dead fish, flies, mosquitoes, ant
# swarms and a roaring tiger were left out on purpose.
PHOTOS = {
    # animals
    "alexandre-boucey-CDxeDdf9WB4-unsplash.jpg": ("animals", "cat", "Cat"),
    "andreea-v-smgqIwTvf0M-unsplash.jpg": ("animals", "cat", "Fluffy cat"),
    "anna-stampfli-X9KcXxvP3II-unsplash.jpg": ("animals", "dog", "Dog"),
    "anusha-barwa-ppKcYi1CXcI-unsplash.jpg": ("animals", "cat", "Cat and dog"),
    "atharva-dixit-yZYfXUcfie0-unsplash.jpg": ("animals", "monkey", "Monkey"),
    "ayla-meinberg-AL2-t0GrSko-unsplash.jpg": ("animals", "cat", "Cat on a walk"),
    "blog-region-Ys5twF343HE-unsplash.jpg": ("animals", "monkey", "Monkey on a roof"),
    "cedric-vt-IuJc2qh2TcA-unsplash.jpg": ("animals", "cat", "Cat"),
    "daniel-castillo-ML2v9PSET-c-unsplash.jpg": ("animals", "monkey", "Monkeys"),
    "daniel-pell-S1qjvxRnZMU-unsplash.jpg": ("animals", "monkey", "Monkey"),
    "daniele-franchi-N-4lwl43-tQ-unsplash.jpg": ("animals", "dog", "Sleepy dog"),
    "divide-by-zero-FGkNt8tO04I-unsplash.jpg": ("animals", "deer", "Deer"),
    "donnie-ray-crisp-66zrT0dJ7Mc-unsplash.jpg": ("animals", "tiger", "Tiger"),
    "erika-fletcher-wHdFa4F1zRA-unsplash.jpg": ("animals", "deer", "Baby deer"),
    "ervin-lukacs-xXz9Z_EEX2I-unsplash.jpg": ("animals", "cow", "Cow"),
    "farzane-hashemi-6e8L4-jPBlg-unsplash.jpg": ("animals", "cow", "Cow"),
    "francois-germain-ApLSGTbbyQU-unsplash.jpg": ("animals", "cow", "Cow"),
    "francois-germain-w_PQddj2fUs-unsplash.jpg": ("animals", "cow", "Cow"),
    "heshan-weeramanthri-rKf9c9-ICGc-unsplash.jpg": ("animals", "dog", "Dogs"),
    "jorge-salvador-WUWGO6xmvoY-unsplash.jpg": ("animals", "goat", "Goats"),
    "jorge-salvador-tqXeLOBzJsI-unsplash.jpg": ("animals", "goat", "Goats"),
    "julie-marsh-TlwzgNd54Pw-unsplash.jpg": ("animals", "deer", "Baby deer"),
    "kabo-ng9yenZfeuI-unsplash.jpg": ("animals", "cat", "Ginger cat"),
    "kabo-p6yH8VmGqxo-unsplash.jpg": ("animals", "cat", "Ginger cat"),
    "matteo-galeazzi-DenYLSCHcDc-unsplash.jpg": ("animals", "deer", "Deer"),
    "matthew-spiteri-WfZ4WCuNtlg-unsplash.jpg": ("animals", "elephant", "Elephants"),
    "mike-marrah-gRB4Euk4BYQ-unsplash.jpg": ("animals", "tiger", "Tiger"),
    "mylon-ollila-j4ocWYAP_cs-unsplash.jpg": ("animals", "elephant", "Elephants"),
    "nam-anh-QJbyG6O0ick-unsplash.jpg": ("animals", "elephant", "Elephant"),
    "nandhu-kumar-jAMcUbsTvWE-unsplash.jpg": ("animals", "goat", "Goat"),
    "nnenna-e-_JWqhi8iRlw-unsplash.jpg": ("animals", "monkey", "Monkey"),
    "paarth-sigdel-MzYv6HoglsM-unsplash.jpg": ("animals", "monkey", "Monkey"),
    "ratanjot-singh-UIhc7sohnvc-unsplash.jpg": ("animals", "tiger", "Tiger drinking"),
    "rishabh-pandoh--iZV3CqT7LM-unsplash.jpg": ("animals", "tiger", "Tiger"),
    "robby-mccullough-obxx8cntJwQ-unsplash.jpg": ("animals", "cat", "Wild cat"),
    "robert-woeger-9t8zwUBy8Uw-unsplash.jpg": ("animals", "deer", "Baby deer"),
    "sai-kamal-oMLVkPJZCVc-unsplash.jpg": ("animals", "dog", "Puppy"),
    "sandaru-muthuwadige-oVo2nZnClRQ-unsplash.jpg": ("animals", "goat", "Goat"),
    "seth-doyle-sXU6BeWoZqI-unsplash.jpg": ("animals", "dog", "Happy dog"),
    "thomas-de-fretes-QRsDpGPV7qk-unsplash.jpg": ("animals", "cat", "Kittens"),
    "wietse-jongsma-uktMaRFDwF8-unsplash.jpg": ("animals", "cow", "Cows"),
    "wolfgang-hasselmann-P7L5011nD5s-unsplash.jpg": ("animals", "elephant", "Elephant"),
    "zaynal-abedin-OIV9p2uL4H8-unsplash.jpg": ("animals", "goat", "Black goat"),
    "zoe-reeve-9hSejnboeTY-unsplash.jpg": ("animals", "elephant", "Elephant"),
    # birds
    "a-perry-jm2kbaUOkr0-unsplash.jpg": ("birds", "crow", "Crow"),
    "aditya-tma-fII_N53Xk7k-unsplash.jpg": ("birds", "chicken", "Hen and chicks"),
    "akshay-madan-Ugl5m_JgBvI-unsplash.jpg": ("birds", "parrot", "Parrot"),
    "anastasiya-romanova-exu0DlNu1Vc-unsplash.jpg": ("birds", "sparrow", "Sparrow"),
    "andy-holmes-i-7HDFvmI6E-unsplash.jpg": ("birds", "sparrow", "Sparrow"),
    "aniket-solankar-Eb_2ItJvcI8-unsplash.jpg": ("birds", "sparrow", "Sparrow"),
    "ben-moreland-auijD19Byq8-unsplash.jpg": ("birds", "chicken", "Hens"),
    "cedric-vt-CpYPdM1_kYQ-unsplash.jpg": ("birds", "bird", "Flying birds"),
    "den-trushtin-zvjsy_1NdyY-unsplash.jpg": ("birds", "parrot", "Budgie"),
    "doorkeepers-iE6lQhAFJ10-unsplash.jpg": ("birds", "crow", "Crow"),
    "dusan-veverkolog-Fxg394rc6JQ-unsplash.jpg": ("birds", "chicken", "Rooster"),
    "fareed-akhyear-chowdhury-kHrv3k4d3EM-unsplash.jpg": ("birds", "bird", "Little bird"),
    "finn-mund-PEzx3drPCrY-unsplash.jpg": ("birds", "chicken", "Rooster"),
    "isaac-martin-X-El5Ad0uyE-unsplash.jpg": ("birds", "bird", "Heron"),
    "jana-klouckova-kudrnova-ZE1w7rIL9xA-unsplash.jpg": ("birds", "bird", "Flying birds"),
    "jonathan-ardila-YMdLSr1VqAA-unsplash.jpg": ("birds", "duck", "White duck"),
    "joydeep-sensarma-pFXJ777sOeM-unsplash.jpg": ("birds", "bird", "Heron"),
    "kai-dahms-Drx2tzJDCOg-unsplash.jpg": ("birds", "duck", "White duck"),
    "kasturi-roy-a1LVsvM_zuE-unsplash.jpg": ("birds", "crow", "Crow"),
    "lukasz-rawa-2BugsxVlrMQ-unsplash.jpg": ("birds", "crow", "Crow"),
    "marshall-patterson-3lnIIskutWo-unsplash.jpg": ("birds", "sparrow", "Sparrow"),
    "md-jahid-hossen-Jexn51nOg9Y-unsplash.jpg": ("birds", "bird", "Magpie robin"),
    "miad-khan-nAiepb2WuCk-unsplash.jpg": ("birds", "bird", "Magpie robin"),
    "michael-starkie-LK4po81NIg4-unsplash.jpg": ("birds", "bird", "Wading bird"),
    "monika-kubala-DIjzFYrzHVw-unsplash.jpg": ("birds", "chicken", "Hen"),
    "pete-nuij-c3NMEtK9fuI-unsplash.jpg": ("birds", "crow", "Crow"),
    "ravi-singh-rN3dqzDrhdk-unsplash.jpg": ("birds", "duck", "Duck"),
    "renan-brun-MHQm4dFkJEA-unsplash.jpg": ("birds", "parrot", "Parrot"),
    "robert-thiemann--ZSnI9gSX1Y-unsplash.jpg": ("birds", "duck", "Ducks"),
    "ross-sokolovski-kCZSzqvIei4-unsplash.jpg": ("birds", "duck", "Duck"),
    "rossano-d-angelo-kWILCdSctPI-unsplash.jpg": ("birds", "bird", "Magpie"),
    "sreenivas-LqJSb6gU1v0-unsplash.jpg": ("birds", "parrot", "Parrot"),
    "sreenivas-a1f-hNNoMGg-unsplash.jpg": ("birds", "parrot", "Parrot"),
    "thomas-iversen-4W8FgDVyUME-unsplash.jpg": ("birds", "chicken", "Hens"),
    "viswaprem-anbarasapandian-Jhtzdnq9YtA-unsplash.jpg": ("birds", "bird", "Myna"),
    "viswaprem-anbarasapandian-jpBUbiBqhBQ-unsplash.jpg": ("birds", "bird", "Myna"),
    "zdenek-machacek-OlKkCmToXEs-unsplash.jpg": ("birds", "parrot", "Parrot"),
    # critters
    "aaron-burden--TYvt5pmKng-unsplash.jpg": ("critters", "butterfly", "Butterfly"),
    "aaron-burden-6csuZQ9oZcI-unsplash.jpg": ("critters", "bee", "Bee on a flower"),
    "adam-currie-8T7oWwnru18-unsplash.jpg": ("critters", "frog", "Frog"),
    "alexey-savchenko-f6DgIqdayMk-unsplash.jpg": ("critters", "snail", "Snail"),
    "anne-lambeck-5VC4thmwMms-unsplash.jpg": ("critters", "butterfly", "Blue butterfly"),
    "atharva-kanekar-Rjf2JCdJv7E-unsplash.jpg": ("critters", "lizard", "Lizard"),
    "atharva-kanekar-aU5E_K8QzrM-unsplash.jpg": ("critters", "lizard", "Monitor lizard"),
    "azzam-qourti-5IV6_bnr7wg-unsplash.jpg": ("critters", "ant", "Ants"),
    "balaji-malliswamy-tD5iUvvsALY-unsplash.jpg": ("critters", "lizard", "Monitor lizard"),
    "boris-smokrovic-gr7ZkoZnHXU-unsplash.jpg": ("critters", "bee", "Bee"),
    "brigitte-elsner-YFb6orz3AQc-unsplash.jpg": ("critters", "dragonfly", "Damselfly"),
    "cherre-bezerra-da-silva-amB0TvcByMI-unsplash.jpg": ("critters", "bee", "Wasp"),
    "dmitry-grigoriev-yxXpjF-RrnA-unsplash.jpg": ("critters", "bee", "Bumblebee"),
    "erik-karits-yCVcYzNYSwQ-unsplash.jpg": ("critters", "lizard", "Lizard"),
    "erzsebet-vehofsics-pcrk6zbQdgo-unsplash.jpg": ("critters", "frog", "Frog"),
    "fatima-garcia-kq1gU_xolXI-unsplash.jpg": ("critters", "lizard", "Monitor lizard"),
    "james-tiono-_LrA3UjfggM-unsplash.jpg": ("critters", "lizard", "Gecko"),
    "jenny-chambers-WIGe9QMVclM-unsplash.jpg": ("critters", "ant", "Ant"),
    "john-cameron-c6OumsbBux8-unsplash.jpg": ("critters", "frog", "Toad"),
    "jose-froilan-s-diaz-o_tJ7cit-Iw-unsplash.jpg": ("critters", "lizard", "Gecko"),
    "joshua-manjgo-bBcIWfrqyv8-unsplash.jpg": ("critters", "ant", "Ants"),
    "jude-infantini-IBhP0x5lZKc-unsplash.jpg": ("critters", "dragonfly", "Dragonfly"),
    "julian-vbax-mLGrSs-unsplash.jpg": ("critters", "snail", "Snail"),
    "krzysztof-niewolny-6-pcQrF0doI-unsplash.jpg": ("critters", "snail", "Snail"),
    "luke-brugger-SHpbGW-sSYE-unsplash.jpg": ("critters", "snail", "Snail"),
    "lydia-dumont-mppXXa6d4_c-unsplash.jpg": ("critters", "frog", "Frog"),
    "marina-grynykha-TZgcKhUWyWo-unsplash.jpg": ("critters", "snail", "Snail"),
    "mathijs-de-koning-kwv6BSKYRuk-unsplash.jpg": ("critters", "dragonfly", "Dragonfly"),
    "meggyn-pomerleau-hAYy2mFLjS8-unsplash.jpg": ("critters", "bee", "Bees"),
    "nayem-islam-RkiJUag9-5k-unsplash.jpg": ("critters", "dragonfly", "Dragonfly"),
    "peter-burne-DaFlkW2XyAE-unsplash.jpg": ("critters", "dragonfly", "Dragonfly"),
    "peter-f-wolf-XG8eYNYdz54-unsplash.jpg": ("critters", "ant", "Ant"),
    "peter-law-i3T8gp9dUVk-unsplash.jpg": ("critters", "frog", "Frog"),
    "pierre-bamin-CsLAqzkPTGw-unsplash.jpg": ("critters", "frog", "Frog"),
    "stefan-richter-PtY3S41RrbM-unsplash.jpg": ("critters", "lizard", "Lizard"),
    "svenja-wagenseil-IaI7GIzjrUU-unsplash.jpg": ("critters", "lizard", "Gecko"),
    "yuriy-vertikov-Cr2jxG24aSo-unsplash.jpg": ("critters", "fish", "Fish"),
    "zdenek-machacek-_9bRrDyOQTQ-unsplash.jpg": ("critters", "snail", "Snail"),
    # vehicles
    "agraj-singh-Y-cwJFLKGPs-unsplash.jpg": ("vehicles", "motorbike", "Motorbike"),
    "ahamed-rasel-VkQ0-G_V7wU-unsplash.jpg": ("vehicles", "train", "Train in the forest"),
    "annie-xia--bpMPG0lpvg-unsplash.jpg": ("vehicles", "boat", "Speedboat"),
    "ashique-anan-abir-pMf7c5w7Dmc-unsplash.jpg": ("vehicles", "train", "Train"),
    "ashraful-haque-akash-8FI5IiikLko-unsplash.jpg": ("vehicles", "boat", "Sailing boat"),
    "austin-curtis-sk8LPCMvw_A-unsplash.jpg": ("vehicles", "boat", "Boats"),
    "bornil-amin-Yg3nh2HlXyA-unsplash.jpg": ("vehicles", "rickshaw", "Auto rickshaw"),
    "bornil-amin-ppE54bv7bFk-unsplash.jpg": ("vehicles", "truck", "Truck"),
    "corey-willett-pYVP7Go8Bzs-unsplash.jpg": ("vehicles", "car", "Car"),
    "creighton-guo-EcfPgSJKHzY-unsplash.jpg": ("vehicles", "train", "Train"),
    "evan-clay-JM8NVuW6e0k-unsplash.jpg": ("vehicles", "car", "Car"),
    "evgeniy-bezkorovayniy-lUPs8gvU6CY-unsplash.jpg": ("vehicles", "car", "Old car"),
    "frederik-lower-qLdFdDNE69w-unsplash.jpg": ("vehicles", "boat", "Boat"),
    "gabor-szuts-K1wVfwihkco-unsplash.jpg": ("vehicles", "bus", "Bus"),
    "hyundai-motor-group-ynX19rbl1PI-unsplash.jpg": ("vehicles", "car", "Car"),
    "isfak-himu-qlqZtci5dLc-unsplash.jpg": ("vehicles", "rickshaw", "Rickshaw"),
    "ivan-ragozin-o9oQaOGpLz0-unsplash.jpg": ("vehicles", "boat", "Speedboat"),
    "jef-van-cleynenbreugel-VBVJ9DVpVQw-unsplash.jpg": ("vehicles", "boat", "Ferry"),
    "johaer-SWclRtrD7sc-unsplash.jpg": ("vehicles", "rickshaw", "Rickshaw"),
    "julian-dik-Be99QIIBIeM-unsplash.jpg": ("vehicles", "train", "Train station"),
    "kabiur-rahman-riyad-6MXbhc047_Y-unsplash.jpg": ("vehicles", "boat", "Boat"),
    "kabiur-rahman-riyad-UY1bkFFYCEE-unsplash.jpg": ("vehicles", "bus", "Red buses"),
    "kalden-swart-0ktrhGN_0fQ-unsplash.jpg": ("vehicles", "train", "Steam train"),
    "lei-hwang-ssGrak-xfeM-unsplash.jpg": ("vehicles", "rickshaw", "Horse cart"),
    "lucia-lua-ramirez-lG0AHN1Gapw-unsplash.jpg": ("vehicles", "bus", "Yellow bus"),
    "maksym-tymchyk-BBgqZzMu3a0-unsplash.jpg": ("vehicles", "motorbike", "Motorbike"),
    "marwan-ahmed-XmGDGbpO40g-unsplash.jpg": ("vehicles", "rickshaw", "Auto rickshaws"),
    "matheus-bardemaker-DIZZa_CoT9Q-unsplash.jpg": ("vehicles", "car", "Car"),
    "maxim-simonov-pJvWS0xjUec-unsplash.jpg": ("vehicles", "motorbike", "Motorbike"),
    "mohammad-samir-HEkomML44dM-unsplash.jpg": ("vehicles", "truck", "Truck"),
    "motoculturel-N13ogsdkkzU-unsplash.jpg": ("vehicles", "motorbike", "Motorbikes"),
    "nandhakumar-s-Y6nGqtDQ0XE-unsplash.jpg": ("vehicles", "train", "Old train"),
    "neha-maheen-mahfin-ELDSG6MfCZ8-unsplash.jpg": ("vehicles", "boat", "Boats"),
    "nejc-soklic-3mwt2iFSWfA-unsplash.jpg": ("vehicles", "train", "Train"),
    "riashat-rafat-Ph5VL5Tilto-unsplash.jpg": ("vehicles", "boat", "Boat on the beach"),
    "roger-starnes-sr-NY6QhxKztOo-unsplash.jpg": ("vehicles", "car", "Old car"),
    "ruben-mavarez-IzXNLHYMUbE-unsplash.jpg": ("vehicles", "bus", "Bus"),
    "rusty-watson-QY6qoCITDh0-unsplash.jpg": ("vehicles", "boat", "Speedboat"),
    "saksham-vikram-irp1v4CMzzE-unsplash.jpg": ("vehicles", "rickshaw", "Auto rickshaw"),
    "sebastian-monroy-OSY7vlpZhFg-unsplash.jpg": ("vehicles", "car", "Blue car"),
    "sehajpal-singh-YrxwT1ytR8U-unsplash.jpg": ("vehicles", "rickshaw", "Auto rickshaw"),
    "shohidul-alam-MmAkXa1v94s-unsplash.jpg": ("vehicles", "bus", "Bus"),
    "simas-j-LIbBfaVLKgM-unsplash.jpg": ("vehicles", "car", "Car"),
    "teddy-o-jtpcrnqP2Mc-unsplash.jpg": ("vehicles", "bus", "Bus in the city"),
    "terry-montague-lYFIsPhotkY-unsplash.jpg": ("vehicles", "motorbike", "Motorbike"),
    "tsuyoshi-kozu-HSJ-HlGmgFI-unsplash.jpg": ("vehicles", "train", "Red train"),
    "ugur-celik-S3FbNSQDM4s-unsplash.jpg": ("vehicles", "car", "Car"),
    "valentin-lisov-d-AIKKqOlTE-unsplash.jpg": ("vehicles", "car", "Red car"),
    "vsevolod-PUOQLdE0T6w-unsplash.jpg": ("vehicles", "boat", "Rowing boat"),
    "wadud-muktadir-XJt6wf0LHBg-unsplash.jpg": ("vehicles", "train", "Train"),
    "zoshua-colah-9WJVzRPjqpk-unsplash.jpg": ("vehicles", "rickshaw", "Auto rickshaws"),
    "zoshua-colah-gL6YBJQaY4c-unsplash.jpg": ("vehicles", "rickshaw", "Auto rickshaw"),
    # village
    "abdullah-arain-lhKhfQSHGkE-unsplash.jpg": ("village", "people", "Boy and goat"),
    "asiqur-rahman-x_1IxCg0siU-unsplash.jpg": ("village", "people", "Fishing net"),
    "aslam-salfi-v5YB-y7Tbww-unsplash.jpg": ("village", "people", "Cows at sunset"),
    "fahaduzzaman-fahad-YOFkNELtNDo-unsplash.jpg": ("village", "people", "Boatman"),
    "hari-gaddigopula-xZEYonpj41o-unsplash.jpg": ("village", "people", "Farmer"),
    "juairia-islam-shefa-1Q9qvYNhVSY-unsplash.jpg": ("village", "people", "Bicycle"),
    "kabiur-rahman-riyad-BkcXo0cUG4c-unsplash.jpg": ("village", "people", "Fishermen"),
    "lewis-j-goetz-xY79odmoO7w-unsplash.jpg": ("village", "people", "Man and dog"),
    "lisa-alam-DE51nul8T5Q-unsplash.jpg": ("village", "people", "Rickshaw puller"),
    "liu-water-pK70TlchTnM-unsplash.jpg": ("village", "people", "Boy and lamb"),
    "shahariar-nerov-ZF6Z57qRBko-unsplash.jpg": ("village", "people", "Cycling"),
    # food, body and everyday things (second batch; a branded candy box and an unclear basket left out)
    "ahmadreza-rezaie-eU2s_fonJkg-unsplash.jpg": ("food", "milk", "Milk"),
    "aleksandar-kuresevic-F2wxdilfgjs-unsplash.jpg": ("body", "beard", "Beard"),
    "amir-esrafili-jbWCiZ6MU-c-unsplash.jpg": ("body", "smile", "Big smile"),
    "antonio-castellano-SZ6dkrFwCbY-unsplash.jpg": ("food", "jackfruit", "Jackfruit"),
    "assad-tanoli-p5s10b6QGQQ-unsplash.jpg": ("village", "people", "Grandfather"),
    "desirae-hayes-vitor-vxtBBfMTMZ0-unsplash.jpg": ("food", "mango", "Mango"),
    "engin-akyurt-PCpoG06fcUI-unsplash.jpg": ("things", "water", "Glass of water"),
    "giorgio-trovato-fczCr7MdE7U-unsplash.jpg": ("food", "banana", "Bananas"),
    "inna-safa-7uAHbj6lyqI-unsplash.jpg": ("food", "rice", "Rice"),
    "julia-zolotova-M_xIaxQE3Ms-unsplash.jpg": ("food", "fruits", "Fruits"),
    "kevin-kevin-LCaBh7QSGr8-unsplash.jpg": ("food", "rice", "Rice"),
    "nishaan-ahmed-KFIYWH03M9Y-unsplash.jpg": ("food", "rice", "Rice and fish"),
    "pushpak-dsilva-r-hQw_obFd0-unsplash.jpg": ("food", "chocolate", "Chocolate"),
    "rodrigo-dos-reis-DkTuGvgPotA-unsplash.jpg": ("food", "banana", "Lots of bananas"),
    "roman-marchenko-Tin0iDzvfDE-unsplash.jpg": ("things", "toothbrush", "Toothbrush"),
    "sara-groblechner-7TgbRVEYdYY-unsplash.jpg": ("things", "toothbrush", "Toothbrushes"),
    "tetiana-bykovets-H22N-9s8AUw-unsplash.jpg": ("food", "chocolate", "Chocolate"),
    "towfiqu-barbhuiya-9PlHtc53NM0-unsplash.jpg": ("food", "jackfruit", "Jackfruit"),
    "utkarxh-rathore-DmBtO0VTvaY-unsplash.jpg": ("body", "eye", "Eye"),
    "z-lh-ZwzIFSgSqOI-unsplash.jpg": ("food", "jackfruit", "Jackfruit tree"),
}


# Left out on review: TotPocket shows no adult women, and no man uncovered between navel and knee
# (e.g. a lungi hitched above the knee). Hands of unknown gender and crowded streets are left out
# too, to be safe.
MODESTY_EXCLUDED = {
    "engin-akyurt-PCpoG06fcUI-unsplash.jpg",  # woman holding a glass of water
    "johaer-SWclRtrD7sc-unsplash.jpg",  # rickshaw puller, legs uncovered
    "lei-hwang-ssGrak-xfeM-unsplash.jpg",  # horse cart passengers
    "bornil-amin-ppE54bv7bFk-unsplash.jpg",  # crowded street
    "asiqur-rahman-x_1IxCg0siU-unsplash.jpg",  # fisherman, lungi above the knee
    "fahaduzzaman-fahad-YOFkNELtNDo-unsplash.jpg",  # boatman, lungi above the knee
    "kabiur-rahman-riyad-BkcXo0cUG4c-unsplash.jpg",  # fishermen in the water
    "lisa-alam-DE51nul8T5Q-unsplash.jpg",  # rickshaw puller sitting
}


def credit(file_name: str) -> dict:
    stem = file_name.rsplit(".", 1)[0]
    if not stem.endswith("-unsplash"):
        return {"author": None, "source": None, "license": None, "url": None}
    stem = stem[: -len("-unsplash")]
    photo_id, author_slug = stem[-11:], stem[:-12]
    return {
        "author": " ".join(part.capitalize() for part in author_slug.split("-") if part),
        "source": "Unsplash",
        "license": "Unsplash License",
        "url": f"https://unsplash.com/photos/{photo_id}",
    }


def commons_photos() -> dict:
    """Approved Wikimedia Commons photos (tools/commons_photos.json), keyed by their cached file."""
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import fetch_commons_photos as commons

    photos = {}
    for entry in json.loads(commons.MANIFEST.read_text()) if commons.MANIFEST.exists() else []:
        if not entry.get("approved"):
            continue
        commons.CACHE.mkdir(exist_ok=True)
        path = commons.download(entry)
        photos[str(path)] = (
            entry["category"],
            entry["subject"],
            entry["title"],
            {"author": entry["author"], "source": "Wikimedia Commons", "license": entry["license"], "url": entry["page"]},
        )
    return photos


def webp(image: Image.Image, edge: int, quality: int) -> tuple[bytes, int, int]:
    copy = image.copy()
    copy.thumbnail((edge, edge), Image.LANCZOS)
    buffer = io.BytesIO()
    copy.save(buffer, "WEBP", quality=quality, method=6)
    return buffer.getvalue(), copy.width, copy.height


def main() -> None:
    source = Path(sys.argv[1]).expanduser() if len(sys.argv) > 1 else Path.home() / "Downloads"
    photos = {name: (*info, credit(name)) for name, info in PHOTOS.items() if name not in MODESTY_EXCLUDED}
    photos.update(commons_photos())
    counters: dict[str, int] = defaultdict(int)
    entries, videos = [], defaultdict(list)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUT, "w", compression=zipfile.ZIP_STORED) as pack:
        for name in sorted(photos, key=lambda n: (list(CATEGORIES).index(photos[n][0]), photos[n][1], n)):
            category, subject, title, photo_credit = photos[name]
            path = Path(name) if Path(name).is_absolute() else source / name
            if not path.exists():
                print(f"missing, skipped: {name}")
                continue
            counters[subject] += 1
            photo_id = f"{subject}-{counters[subject]:02d}"
            image = ImageOps.exif_transpose(Image.open(path)).convert("RGB")
            full, width, height = webp(image, FULL_EDGE, FULL_QUALITY)
            thumb, _, _ = webp(image, THUMB_EDGE, THUMB_QUALITY)
            image_path = f"images/{category}/{photo_id}.webp"
            thumb_path = f"thumbs/{category}/{photo_id}.webp"
            pack.writestr(image_path, full)
            pack.writestr(thumb_path, thumb)
            entries.append({
                "id": f"{category}/{photo_id}",
                "category": category,
                "subject": subject,
                "title": title,
                "image": image_path,
                "thumb": thumb_path,
                "width": width,
                "height": height,
                "sound": SUBJECTS[subject][1],
                "credit": photo_credit,
            })
            videos[subject].append(f"{category}/{photo_id}")
            print(f"{photo_id:14} {len(full) // 1024:4d} KB  {title}")

        catalog = {
            "version": PACK_VERSION,
            "categories": [
                {"id": cid, "title": title, "cover": next(e["id"] for e in entries if e["category"] == cid)}
                for cid, title in CATEGORIES.items()
                if any(e["category"] == cid for e in entries)
            ],
            "photos": entries,
            "videos": [
                {
                    "id": subject,
                    "title": SUBJECTS[subject][0],
                    "category": next(e["category"] for e in entries if e["id"] == ids[0]),
                    "channel": f"TotPocket {CATEGORIES[next(e['category'] for e in entries if e['id'] == ids[0])]}",
                    "photos": ids,
                }
                for subject, ids in videos.items()
                if len(ids) >= 3
            ],
        }
        pack.writestr("catalog.json", json.dumps(catalog, indent=2, ensure_ascii=False))

    size = OUT.stat().st_size / 1024 / 1024
    print(f"\n{len(entries)} photos, {len(catalog['videos'])} videos -> {OUT.relative_to(ROOT)} ({size:.1f} MB)")


if __name__ == "__main__":
    main()
