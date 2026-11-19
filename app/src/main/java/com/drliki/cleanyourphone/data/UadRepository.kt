package com.drliki.cleanyourphone.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.drliki.cleanyourphone.model.CategorizedPackages
import com.drliki.cleanyourphone.model.DevicePackage
import com.drliki.cleanyourphone.model.PROTECTED_PACKAGES
import com.drliki.cleanyourphone.model.PackageCategory
import com.drliki.cleanyourphone.model.RemovalTier
import com.drliki.cleanyourphone.model.UadEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UadRepository(private val context: Context) {

    val settings = AppSettings(context)
    private val json = Json { ignoreUnknownKeys = true }
    private var catalog: Map<String, UadEntry> = emptyMap()

    companion object {
        const val UAD_NG_REMOTE_URL = AppSettings.DEFAULT_UAD_REPO_URL
        const val CLEANYOURTV_REMOTE_URL = AppSettings.DEFAULT_TV_REPO_URL
        private const val CACHE_FILE_NAME = "uad_lists.json"
        private const val CACHE_TV_FILE_NAME = "cleanyourtv_lists.json"

        private val KNOWN_APP_LABELS = mapOf(
            // Samsung & One UI
            "com.sec.android.app.sbrowser" to "Samsung Internet",
            "com.sec.android.app.sbrowser.beta" to "Samsung Internet Beta",
            "com.sec.android.app.popupcalculator" to "Samsung Calculator",
            "com.sec.android.app.myfiles" to "Samsung My Files",
            "com.sec.android.app.voicenote" to "Samsung Voice Recorder",
            "com.sec.android.app.camera" to "Samsung Camera",
            "com.sec.android.gallery3d" to "Samsung Gallery",
            "com.sec.android.app.clockpackage" to "Samsung Clock",
            "com.samsung.android.app.notes" to "Samsung Notes",
            "com.samsung.android.calendar" to "Samsung Calendar",
            "com.samsung.android.messaging" to "Samsung Messages",
            "com.samsung.android.dialer" to "Samsung Phone",
            "com.samsung.android.incallui" to "Samsung In-Call UI",
            "com.samsung.android.contacts" to "Samsung Contacts",
            "com.samsung.android.app.contacts" to "Samsung Contacts",
            "com.samsung.android.app.telephonyui" to "Samsung Call Settings",
            "com.samsung.android.voc" to "Samsung Members",
            "com.samsung.android.app.spage" to "Samsung Free",
            "com.samsung.android.game.gos" to "Game Optimizing Service (GOS)",
            "com.samsung.android.game.gamehome" to "Gaming Hub / Launcher",
            "com.samsung.android.game.gametools" to "Game Booster",
            "com.samsung.android.bixby.agent" to "Bixby Voice",
            "com.samsung.android.bixby.wakeup" to "Bixby Wakeup",
            "com.samsung.android.bixby.service" to "Bixby Service",
            "com.samsung.android.visionintelligence" to "Bixby Vision",
            "com.samsung.android.app.routines" to "Bixby Routines",
            "com.samsung.android.smartswitchassistant" to "Smart Switch",
            "com.sec.android.easyMover" to "Smart Switch Agent",
            "com.samsung.android.oneconnect" to "SmartThings",
            "com.samsung.android.rubin.app" to "Customization Service (Rubin)",
            "com.samsung.android.kidsinstaller" to "Samsung Kids",
            "com.samsung.android.app.sharelive" to "Quick Share",
            "com.samsung.android.allshare.service.fileshare" to "Quick Share File Service",
            "com.samsung.android.aware.service" to "Quick Share Core",
            "com.samsung.android.ardrawing" to "AR Doodle",
            "com.samsung.android.aremoji" to "AR Emoji",
            "com.samsung.android.aremojieditor" to "AR Emoji Editor",
            "com.samsung.android.arzone" to "AR Zone",
            "com.samsung.android.livestickers" to "AR Live Stickers",
            "com.samsung.android.spay" to "Samsung Wallet",
            "com.samsung.android.samsungpass" to "Samsung Pass",
            "com.samsung.android.samsungpassautofill" to "Samsung Pass Autofill",
            "com.samsung.android.app.tips" to "Samsung Tips",
            "com.samsung.android.weather" to "Samsung Weather",
            "com.samsung.android.mdecservice" to "Call & Text on other devices",
            "com.sec.android.app.samsungapps" to "Galaxy Store",
            "com.sec.android.app.billing" to "Samsung Checkout",
            "com.samsung.android.honeyboard" to "Samsung Keyboard",
            "com.samsung.android.knox.containercore" to "Knox Container Core",
            "com.samsung.knox.securefolder" to "Secure Folder",
            "com.sec.enterprise.knox.attestation" to "Knox Attestation",
            "com.samsung.android.app.smartcapture" to "Samsung Smart Capture",
            "com.samsung.android.lool" to "Device Care",
            "com.samsung.android.sm.devicesecurity" to "Device Security (McAfee)",
            "com.samsung.android.app.dressroom" to "Samsung Wallpaper & Themes",
            "com.samsung.android.themestore" to "Galaxy Themes",
            "com.samsung.android.themecenter" to "Galaxy Themes Core",
            "com.samsung.android.dynamiclock" to "Dynamic Lock Screen",
            "com.samsung.android.app.cocktailbarservice" to "Edge Panels",
            "com.samsung.android.fmm" to "Find My Mobile",
            "com.samsung.android.mdx" to "Multi Control / Link to Windows",
            "com.samsung.android.dha" to "Samsung Adapt Sound",
            "com.samsung.android.mobileservice" to "Samsung Experience Service",
            "com.samsung.android.authfw" to "Samsung Authentication Framework",
            "com.samsung.android.fast" to "Samsung Link Sharing",
            "com.samsung.android.app.soundpicker" to "Sound Picker",
            "com.samsung.android.scs" to "Samsung Core Services",
            "com.samsung.android.ipsgeofence" to "Samsung Geo-fence Service",
            "com.samsung.android.location" to "Samsung Location SDK",
            "com.samsung.android.app.simplesharing" to "Simple Sharing",
            "com.samsung.android.privateshare" to "Private Share",
            "com.samsung.android.app.reminder" to "Samsung Reminder",
            "com.samsung.android.forest" to "Digital Wellbeing (Samsung)",
            "com.samsung.android.smartface" to "Smart Stay",
            "com.samsung.android.beaconmanager" to "SmartThings Find Beacon",
            "com.samsung.android.easysetup" to "Nearby Device Scanning",

            // Xiaomi / MIUI / HyperOS
            "com.miui.analytics" to "Xiaomi Analytics",
            "com.miui.msa.global" to "MIUI System Ads (MSA)",
            "com.miui.cleanmaster" to "Cleaner",
            "com.miui.cloudservice" to "Mi Cloud",
            "com.miui.cloudservice.sysbase" to "Mi Cloud Core",
            "com.miui.cloudbackup" to "Mi Cloud Backup",
            "com.miui.yellowpage" to "Yellow Pages",
            "com.miui.videoplayer" to "Mi Video",
            "com.miui.player" to "Mi Music",
            "com.miui.weather2" to "Mi Weather",
            "com.miui.compass" to "Compass",
            "com.miui.calculator" to "Mi Calculator",
            "com.miui.notes" to "Mi Notes",
            "com.miui.gallery" to "Mi Gallery",
            "com.miui.bugreport" to "Services & Feedback",
            "com.miui.home" to "System Launcher (HyperOS)",
            "com.mi.android.globalpersonalassistant" to "App Vault",
            "com.miui.hybrid" to "Quick Apps",
            "com.miui.hybrid.accessory" to "Quick Apps Engine",
            "com.miui.securityadd" to "Security Component",
            "com.miui.securitycenter" to "Security Center",
            "com.miui.powerkeeper" to "Battery & Performance",
            "com.xiaomi.joyose" to "Joyose (Game Throttling)",
            "com.xiaomi.midrop" to "ShareMe",
            "com.xiaomi.mipicks" to "GetApps",
            "com.xiaomi.payment" to "Mi Pay",
            "com.xiaomi.scanner" to "Mi Scanner",
            "com.xiaomi.discover" to "Xiaomi Discover",
            "com.xiaomi.micloud.sdk" to "Mi Cloud SDK",
            "com.xiaomi.miplay_client" to "Mi Play",
            "com.xiaomi.simactivate.service" to "Xiaomi SIM Activation",
            "com.miui.screenrecorder" to "Screen Recorder",
            "com.miui.voiceassist" to "Mi AI Voice Assistant",
            "com.miui.fm" to "FM Radio",
            "com.miui.miservice" to "Mi Service",
            "com.miui.mishare.connectivity" to "Mi Share",
            "com.miui.aod" to "Always-on Display",

            // Google & Android TV
            "com.google.android.gms" to "Google Play Services",
            "com.google.android.gsf" to "Google Services Framework",
            "com.android.vending" to "Google Play Store",
            "com.google.android.youtube" to "YouTube",
            "com.google.android.youtube.tv" to "YouTube on TV",
            "com.google.android.youtube.tvmusic" to "YouTube Music on TV",
            "com.google.android.apps.youtube.music" to "YouTube Music",
            "com.google.android.videos" to "Google TV",
            "com.google.android.apps.photos" to "Google Photos",
            "com.google.android.apps.docs" to "Google Drive",
            "com.google.android.apps.docs.editors.docs" to "Google Docs",
            "com.google.android.apps.docs.editors.sheets" to "Google Sheets",
            "com.google.android.apps.docs.editors.slides" to "Google Slides",
            "com.google.android.apps.maps" to "Google Maps",
            "com.google.android.gm" to "Gmail",
            "com.google.android.googlequicksearchbox" to "Google App",
            "com.google.android.inputmethod.latin" to "Gboard",
            "com.android.chrome" to "Google Chrome",
            "com.google.android.apps.messaging" to "Google Messages",
            "com.google.android.dialer" to "Google Phone",
            "com.google.android.contacts" to "Google Contacts",
            "com.google.android.calendar" to "Google Calendar",
            "com.google.android.keep" to "Google Keep",
            "com.google.android.apps.tachyon" to "Google Meet",
            "com.google.android.apps.wellbeing" to "Digital Wellbeing",
            "com.google.android.apps.nbu.paisa.user" to "Google Pay",
            "com.google.android.apps.nbu.files" to "Files by Google",
            "com.google.android.deskclock" to "Google Clock",
            "com.google.android.calculator" to "Google Calculator",
            "com.google.android.tts" to "Speech Services by Google",
            "com.google.android.marvin.talkback" to "Android Accessibility Suite",
            "com.google.android.tvlauncher" to "Android TV Home",
            "com.google.android.tv.launcher" to "Android TV Launcher",
            "com.google.android.apps.tv.launcherx" to "Google TV Launcher",
            "com.google.android.tvrecommendations" to "Android TV Core Services",
            "com.google.android.katniss" to "Google Assistant on TV",
            "com.google.android.tungsten.setupwraith" to "Android TV Setup",
            "com.google.android.tv.remote.service" to "Android TV Remote Service",
            "com.google.android.syncadapters.calendar" to "Google Calendar Sync",
            "com.google.android.syncadapters.contacts" to "Google Contacts Sync",
            "com.google.android.backuptransport" to "Google Backup Transport",
            "com.google.android.feedback" to "Google Play Feedback",
            "com.google.android.partnersetup" to "Google Partner Setup",
            "com.google.android.printservice.recommendation" to "Print Service Recommendation",
            "com.google.android.onetimeinitializer" to "Google One Time Initializer",
            "com.google.android.ext.services" to "Android Extension Services",
            "com.google.android.ext.shared" to "Android Shared Library",
            "com.google.android.play.games" to "Google Play Games",
            "com.google.android.projection.gearhead" to "Android Auto",
            "com.google.android.apps.fitness" to "Google Fit",

            // Streaming, Media & Entertainment
            "com.netflix.ninja" to "Netflix on TV",
            "com.netflix.mediaclient" to "Netflix",
            "com.amazon.amazonvideo.livingroom" to "Prime Video on TV",
            "com.amazon.avod.thirdpartyclient" to "Prime Video",
            "com.amazon.mp3" to "Amazon Music",
            "com.amazon.mShop.android.shopping" to "Amazon Shopping",
            "com.spotify.music" to "Spotify",
            "com.spotify.tv.android" to "Spotify on TV",
            "com.disney.disneyplus" to "Disney+",
            "com.hulu.plus" to "Hulu",
            "com.hbo.hbonow" to "Max (HBO)",
            "com.wbd.stream" to "Max",
            "com.apple.atve.androidtv.appletv" to "Apple TV",
            "com.peacocktv.peacockandroid" to "Peacock",
            "com.cbs.app" to "Paramount+",
            "com.plexapp.android" to "Plex",
            "org.xbmc.kodi" to "Kodi",
            "tv.twitch.android.app" to "Twitch",
            "com.crunchyroll.crunchyroid" to "Crunchyroll",
            "com.hotstar.androidtv" to "Disney+ Hotstar TV",
            "in.startv.hotstar" to "Disney+ Hotstar",
            "com.jio.media.ondemand" to "JioCinema",
            "com.sonyliv" to "Sony LIV",
            "com.graymatrix.did" to "Zee5",
            "org.videolan.vlc" to "VLC Media Player",
            "com.mxtech.videoplayer.ad" to "MX Player",

            // BBK Electronics (OnePlus, OPPO, Realme, Vivo)
            "com.oneplus.camera" to "OnePlus Camera",
            "com.oneplus.gallery" to "OnePlus Gallery",
            "com.oneplus.soundrecorder" to "OnePlus Sound Recorder",
            "com.oneplus.weather" to "OnePlus Weather",
            "com.oneplus.filemanager" to "OnePlus File Manager",
            "com.oneplus.calculator" to "OnePlus Calculator",
            "com.oneplus.deskclock" to "OnePlus Clock",
            "com.oneplus.note" to "OnePlus Notes",
            "com.oneplus.gamespace" to "OnePlus Games",
            "com.coloros.gamespace" to "OPlus Game Space",
            "com.coloros.athena" to "Athena Resource Manager",
            "com.oplus.cosa" to "OPPO System Assistant",
            "com.heytap.mcs" to "HeyTap Cloud Service",
            "com.heytap.pictorial" to "Lock Screen Magazine",
            "com.heytap.themestore" to "Theme Store",
            "com.heytap.market" to "App Market",
            "com.heytap.browser" to "HeyTap Browser",
            "com.oplus.melody" to "HeyMelody",
            "com.vivo.browser" to "Vivo Browser",
            "com.vivo.easyshare" to "Vivo EasyShare",
            "com.vivo.appstore" to "V-Appstore",
            "com.vivo.globalsearch" to "Vivo Global Search",
            "com.vivo.game" to "Vivo Game Center",
            "com.vivo.space" to "Jovi Home",

            // Motorola, Sony, TCL, Transsion
            "com.motorola.ccc.notification" to "Motorola Notifications",
            "com.motorola.ccc.ota" to "Motorola Software Update",
            "com.motorola.genie" to "Moto Help",
            "com.motorola.motocit" to "Moto Device Test",
            "com.motorola.actions" to "Moto Actions",
            "com.motorola.gestures" to "Moto Gestures",
            "com.motorola.gamemode" to "Moto Gametime",
            "com.motorola.camera2" to "Moto Camera",
            "com.motorola.readyfor" to "Ready For",
            "com.sonymobile.cinema" to "Cinema Pro",
            "com.sonymobile.photopro" to "Photo Pro",
            "com.sonymobile.music" to "Sony Music",
            "com.sony.dtv.irbservice" to "Sony TV IR Blaster",
            "com.sony.dtv.tvx" to "Sony TV Services",
            "com.tcl.tvmanager" to "TCL TV Guard",
            "com.tcl.browser" to "TCL BrowseHere",
            "com.tcl.multiscreen.client" to "TCL MagiConnect",
            "com.transsion.hilauncher" to "HiOS Launcher",
            "com.transsion.xshare" to "XShare",

            // Meta, Microsoft, Common Social & Utilities
            "com.facebook.katana" to "Facebook",
            "com.facebook.orca" to "Messenger",
            "com.facebook.services" to "Meta Services",
            "com.facebook.appmanager" to "Meta App Manager",
            "com.facebook.system" to "Meta App Installer",
            "com.instagram.android" to "Instagram",
            "com.whatsapp" to "WhatsApp",
            "com.microsoft.office.officehubrow" to "Microsoft 365 (Office)",
            "com.microsoft.office.outlook" to "Microsoft Outlook",
            "com.microsoft.skydrive" to "Microsoft OneDrive",
            "com.microsoft.appmanager" to "Link to Windows",
            "com.microsoft.teams" to "Microsoft Teams",
            "com.linkedin.android" to "LinkedIn",
            "org.telegram.messenger" to "Telegram",
            "com.twitter.android" to "X (Twitter)",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.snapchat.android" to "Snapchat",
            "com.discord" to "Discord",
            "us.zoom.videomeetings" to "Zoom",
            "com.truecaller" to "Truecaller",
            "com.shazam.android" to "Shazam",
            "com.booking" to "Booking.com",
            "com.king.candycrushsaga" to "Candy Crush Saga",

            // ── Popular User-Installed Apps (Media, Tools, Browsers, Social, TV, Games) ──
            "org.videolan.vlc" to "VLC Media Player",
            "com.mxtech.videoplayer.ad" to "MX Player",
            "com.mxtech.videoplayer.pro" to "MX Player Pro",
            "org.xbmc.kodi" to "Kodi",
            "com.plexapp.android" to "Plex",
            "com.mb.android" to "Emby",
            "org.jellyfin.mobile" to "Jellyfin",
            "org.jellyfin.androidtv" to "Jellyfin for Android TV",
            "com.stremio.one" to "Stremio",
            "com.teamsmart.videomanager.tv" to "SmartTube",
            "com.liskovsoft.videomanager" to "SmartTube Next",
            "app.revanced.android.youtube" to "YouTube ReVanced",
            "app.revanced.manager.flutter" to "ReVanced Manager",
            "org.schabi.newpipe" to "NewPipe",
            "com.futo.platformplayer" to "Grayjay",
            "com.maxmpz.audioplayer" to "Poweramp",
            "com.maxmpz.audioplayer.unlock" to "Poweramp Full Version Unlocker",
            "in.krosbits.musicolet" to "Musicolet Music Player",
            "code.name.monkey.retromusic" to "Retro Music Player",
            "it.vfsfitvnm.vimusic" to "ViMusic",
            "com.zionhuang.music" to "InnerTune",
            "com.aspiro.tidal" to "Tidal",
            "deezer.android.app" to "Deezer",
            "com.soundcloud.android" to "SoundCloud",
            "com.apple.android.music" to "Apple Music",
            "au.com.shiftyjelly.pocketcasts" to "Pocket Casts",
            "de.danoeh.antennapod" to "AntennaPod",
            "com.brouken.player" to "Just Player",
            "is.xyz.mpv" to "mpv-android",
            "org.courville.nova" to "Nova Video Player",

            // Browsers
            "com.brave.browser" to "Brave Browser",
            "com.brave.browser_nightly" to "Brave Nightly",
            "com.brave.browser_beta" to "Brave Beta",
            "org.mozilla.firefox" to "Firefox",
            "org.mozilla.firefox_beta" to "Firefox Beta",
            "org.mozilla.fenix" to "Firefox Nightly",
            "org.mozilla.focus" to "Firefox Focus",
            "com.opera.browser" to "Opera Browser",
            "com.opera.mini.native" to "Opera Mini",
            "com.opera.gx" to "Opera GX",
            "com.opera.touch" to "Opera Touch",
            "com.vivaldi.browser" to "Vivaldi Browser",
            "com.duckduckgo.mobile.android" to "DuckDuckGo Privacy Browser",
            "com.microsoft.emmx" to "Microsoft Edge",
            "com.kiwibrowser.browser" to "Kiwi Browser",
            "org.torproject.torbrowser" to "Tor Browser",
            "org.torproject.torbrowser_alpha" to "Tor Browser Alpha",
            "com.UCMobile.intl" to "UC Browser",
            "com.phlox.tvwebbrowser" to "TV Bro",

            // File Managers, Tools & Utilities
            "com.lonelycatgames.Xplore" to "X-plore File Manager",
            "com.ghisler.android.TotalCommander" to "Total Commander",
            "pl.solidexplorer2" to "Solid Explorer",
            "com.mixplorer" to "MiXplorer",
            "com.mixplorer.silver" to "MiXplorer Silver",
            "nextapp.fx" to "FX File Explorer",
            "com.cxinventor.file.explorer" to "Cx File Explorer",
            "com.speedsoftware.rootexplorer" to "Root Explorer",
            "com.speedsoftware.explorer" to "Explorer",
            "com.estrongs.android.pop" to "ES File Explorer",
            "ru.zdevs.zarchiver" to "ZArchiver",
            "ru.zdevs.zarchiver.pro" to "ZArchiver Pro",
            "com.rarlab.rar" to "RAR",
            "com.termux" to "Termux",
            "com.termux.api" to "Termux:API",
            "org.fdroid.fdroid" to "F-Droid",
            "org.fdroid.fdroid.privileged.ota" to "F-Droid Privileged Extension",
            "com.aurora.store" to "Aurora Store",
            "com.aurora.droid" to "Aurora Droid",
            "moe.shizuku.privileged.api" to "Shizuku",
            "eu.thedarken.sdm" to "SD Maid",
            "eu.thedarken.sdm.cleanup" to "SD Maid 2 / SE",
            "io.github.muntashirakon.AppManager" to "App Manager",
            "com.topjohnwu.magisk" to "Magisk",
            "me.weishu.kernelsu" to "KernelSU",
            "org.zwanoo.android.speedtest" to "Speedtest by Ookla",
            "com.speedchecker.android.speedchecker" to "Speed Checker",
            "com.yablio.sendfilestotv" to "Send Files to TV",
            "info.androidtv.downloader" to "Downloader by AFTVnews",
            "flar2.homebutton" to "Button Mapper",
            "com.spocky.projengmenu" to "Projectivy Launcher",
            "me.efesser.flauncher" to "FLauncher",
            "ca.dstudio.atvlauncher.pro" to "ATV Launcher Pro",
            "ca.dstudio.atvlauncher.free" to "ATV Launcher",
            "com.teslacoilsw.launcher" to "Nova Launcher",
            "com.teslacoilsw.launcher.prime" to "Nova Launcher Prime",
            "bitpit.launcher" to "Niagara Launcher",
            "ginlemon.flowerfree" to "Smart Launcher",

            // Social & Communication
            "com.whatsapp.w4b" to "WhatsApp Business",
            "org.thunderdog.challegram" to "Telegram X",
            "org.thoughtcrime.securesms" to "Signal",
            "com.reddit.frontpage" to "Reddit",
            "com.rubenmayayo.reddit" to "Boost for Reddit",
            "ml.docilealligator.infinityforreddit" to "Infinity for Reddit",
            "com.laurencedawson.reddit_sync" to "Sync for Reddit",
            "com.slack" to "Slack",
            "com.instagram.barcelona" to "Threads",
            "com.facebook.mlite" to "Messenger Lite",
            "com.facebook.lite" to "Facebook Lite",
            "com.pinterest" to "Pinterest",
            "com.tumblr" to "Tumblr",
            "com.viber.voip" to "Viber",
            "jp.naver.line.android" to "LINE",
            "com.tencent.mm" to "WeChat",
            "com.vkontakte.android" to "VK",

            // Gaming, Streaming & Emulators
            "com.valvesoftware.steamlink" to "Steam Link",
            "com.valvesoftware.android.steam.community" to "Steam",
            "com.moonlight_stream.moonlight" to "Moonlight Game Streaming",
            "com.lizardbyte.sunshine" to "Sunshine Game Stream",
            "org.retroarch" to "RetroArch",
            "org.retroarch.aarch64" to "RetroArch Plus",
            "org.ppsspp.ppsspp" to "PPSSPP",
            "org.ppsspp.ppssppgold" to "PPSSPP Gold",
            "org.dolphinemu.dolphinemu" to "Dolphin Emulator",
            "xyz.aethersx2.android" to "AetherSX2",
            "com.github.stenzek.duckstation" to "DuckStation",
            "org.citra.citra_emu" to "Citra Emulator",
            "org.yuzu.yuzu_emu" to "Yuzu Emulator",
            "com.nvidia.geforcenow" to "GeForce NOW",
            "com.gamepass" to "Xbox Game Pass",
            "com.microsoft.xboxone.smartglass" to "Xbox",
            "com.scee.psxandroid" to "PlayStation App",
            "com.epicgames.portal" to "Epic Games Store",
            "com.roblox.client" to "Roblox",
            "com.mojang.minecraftpe" to "Minecraft",
            "com.dts.freefireth" to "Free Fire",
            "com.dts.freefiremax" to "Free Fire MAX",
            "com.tencent.ig" to "PUBG Mobile",
            "com.pubg.imobile" to "BGMI",
            "com.miHoYo.GenshinImpact" to "Genshin Impact",
            "com.activision.callofduty.shooter" to "Call of Duty: Mobile",

            // Security, Privacy, Password & Cloud
            "com.x8bit.bitwarden" to "Bitwarden",
            "com.agilebits.onepassword" to "1Password",
            "keepass2android.keepass2android" to "KeePass2Android",
            "ch.protonmail.android" to "Proton Mail",
            "ch.protonvpn.android" to "Proton VPN",
            "com.nordvpn.android" to "NordVPN",
            "com.expressvpn.vpn" to "ExpressVPN",
            "net.mullvad.mullvadvpn" to "Mullvad VPN",
            "com.surfshark.vpnclient.android" to "Surfshark VPN",
            "com.wireguard.android" to "WireGuard",
            "com.adguard.android" to "AdGuard",
            "com.adguard.android.contentblocker" to "AdGuard Content Blocker",
            "io.nextdns.NextDNS" to "NextDNS",
            "com.dropbox.android" to "Dropbox",
            "mega.privacy.android.app" to "MEGA",

            // Productivity, Notes & Tasks
            "notion.id" to "Notion",
            "md.obsidian" to "Obsidian",
            "com.todoist" to "Todoist",
            "com.ticktick.task" to "TickTick",
            "com.anydo" to "Any.do",
            "com.ichi2.anki" to "AnkiDroid",
            "com.adobe.reader" to "Adobe Acrobat Reader",
            "com.duolingo" to "Duolingo",
            "com.strava" to "Strava",
            "com.fitbit.FitbitMobile" to "Fitbit",
            "com.niksoftware.snapseed" to "Snapseed",
            "com.waze" to "Waze",
            "com.ubercab" to "Uber",
            "com.airbnb.android" to "Airbnb",
            "com.ebay.mobile" to "eBay",
            "com.alibaba.aliexpresshd" to "AliExpress",
            "com.paypal.android.p2pmobile" to "PayPal",
            "com.revolut.revolut" to "Revolut",
            "com.transferwise.android" to "Wise",
            "com.squareup.cash" to "Cash App",
            "com.venmo" to "Venmo"
        )
    }

    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    private val cacheTvFile: File
        get() = File(context.filesDir, CACHE_TV_FILE_NAME)

    private var tvCatalog: Map<String, UadEntry> = emptyMap()

    suspend fun loadCatalog(): Map<String, UadEntry> = withContext(Dispatchers.IO) {
        if (catalog.isNotEmpty()) return@withContext catalog

        // Check auto-update on launch if configured
        if (settings.autoUpdateOnLaunch) {
            val lastUpdate = settings.lastUpdateTime
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastUpdate > oneDayMs) {
                runCatching { updateFromGitHub() }
            }
        }

        // Try local downloaded cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val raw = cacheFile.readText()
                catalog = json.decodeFromString<Map<String, UadEntry>>(raw)
                if (catalog.isNotEmpty()) return@withContext catalog
            } catch (_: Exception) { }
        }

        // Fallback to bundled asset
        try {
            val raw = context.assets.open("uad_lists_seed.json").bufferedReader().use { it.readText() }
            catalog = json.decodeFromString<Map<String, UadEntry>>(raw)
        } catch (_: Exception) {
            catalog = emptyMap()
        }
        catalog
    }

    suspend fun loadTvCatalog(): Map<String, UadEntry> = withContext(Dispatchers.IO) {
        if (tvCatalog.isNotEmpty()) return@withContext tvCatalog

        // Check auto-update on launch if configured
        if (settings.autoUpdateTvOnLaunch) {
            val lastUpdate = settings.lastTvUpdateTime
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastUpdate > oneDayMs) {
                runCatching { updateTvFromGitHub() }
            }
        }

        // Try local downloaded cache first
        if (cacheTvFile.exists() && cacheTvFile.length() > 0) {
            try {
                val raw = cacheTvFile.readText()
                tvCatalog = json.decodeFromString<Map<String, UadEntry>>(raw)
                if (tvCatalog.isNotEmpty()) return@withContext tvCatalog
            } catch (_: Exception) { }
        }

        // Fallback to bundled asset
        try {
            val raw = context.assets.open("cleanyourtv_lists_seed.json").bufferedReader().use { it.readText() }
            tvCatalog = json.decodeFromString<Map<String, UadEntry>>(raw)
        } catch (_: Exception) {
            tvCatalog = emptyMap()
        }
        tvCatalog
    }

    suspend fun updateFromGitHub(customUrl: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = customUrl?.ifBlank { null } ?: settings.uadRepoUrl
            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "CleanYourPhone-Android")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val downloaded = json.decodeFromString<Map<String, UadEntry>>(body)
            if (downloaded.isEmpty()) {
                throw IllegalStateException("Downloaded catalog is empty")
            }

            // Save to internal storage
            cacheFile.writeText(body)
            catalog = downloaded
            settings.lastUpdateTime = System.currentTimeMillis()
            downloaded.size
        }
    }

    suspend fun updateTvFromGitHub(customUrl: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = customUrl?.ifBlank { null } ?: settings.tvRepoUrl
            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "CleanYourPhone-Android")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val downloaded = json.decodeFromString<Map<String, UadEntry>>(body)
            if (downloaded.isEmpty()) {
                throw IllegalStateException("Downloaded CleanYourTV catalog is empty")
            }

            // Save to internal storage
            cacheTvFile.writeText(body)
            tvCatalog = downloaded
            settings.lastTvUpdateTime = System.currentTimeMillis()
            downloaded.size
        }
    }

    fun getCatalogSize(): Int = catalog.size

    fun getTvCatalogSize(): Int = if (tvCatalog.isNotEmpty()) tvCatalog.size else 2427

    fun isUsingRemoteCache(): Boolean = cacheFile.exists() && cacheFile.length() > 0

    fun isUsingRemoteTvCache(): Boolean = cacheTvFile.exists() && cacheTvFile.length() > 0

    // ── Brand & Model Tailored Recommendation Matcher ───────────────────────
    data class BrandMatcher(
        val displayName: String,
        val keywords: List<String>,
        val packagePrefixes: List<String>
    )

    private val brandMatchers = listOf(
        BrandMatcher("Samsung", listOf("samsung", "sec"), listOf("com.samsung.", "com.sec.", "com.osp.")),
        BrandMatcher("Xiaomi", listOf("xiaomi", "redmi", "poco", "miui", "hyperos"), listOf("com.miui.", "com.xiaomi.", "com.mi.")),
        BrandMatcher("Google Pixel", listOf("google", "pixel"), listOf("com.google.android.apps.")),
        BrandMatcher("OnePlus", listOf("oneplus", "one plus"), listOf("com.oneplus.", "net.oneplus.")),
        BrandMatcher("OPPO", listOf("oppo", "coloros", "heytap"), listOf("com.oppo.", "com.coloros.", "com.heytap.")),
        BrandMatcher("Realme", listOf("realme"), listOf("com.realme.", "com.oppo.", "com.coloros.")),
        BrandMatcher("Vivo", listOf("vivo", "iqoo", "funtouch", "origin"), listOf("com.vivo.", "com.iqoo.", "com.bbk.")),
        BrandMatcher("Motorola", listOf("motorola", "moto", "lenovo"), listOf("com.motorola.", "com.lenovo.")),
        BrandMatcher("Sony", listOf("sony", "xperia", "bravia"), listOf("com.sony.", "com.sonymobile.")),
        BrandMatcher("Huawei/Honor", listOf("huawei", "honor", "emui", "magicui"), listOf("com.huawei.", "com.honor.", "com.hihonor.")),
        BrandMatcher("Transsion (Tecno/Infinix)", listOf("infinix", "tecno", "itel", "transsion", "xos", "hios"), listOf("com.transsion.", "com.infinix.", "com.tecno.")),
        BrandMatcher("Amazon", listOf("amazon", "fire"), listOf("com.amazon.")),
        BrandMatcher("TCL", listOf("tcl"), listOf("com.tcl.")),
        BrandMatcher("ASUS", listOf("asus", "rog", "zenfone"), listOf("com.asus.")),
        BrandMatcher("LG", listOf("lg", "lge"), listOf("com.lge.")),
        BrandMatcher("NVIDIA", listOf("nvidia", "shield"), listOf("com.nvidia."))
    )

    fun findBrandMatcher(brand: String?, manufacturer: String?, model: String?): BrandMatcher? {
        val combined = listOfNotNull(brand, manufacturer, model).joinToString(" ").lowercase()
        if (combined.isBlank()) return null
        return brandMatchers.firstOrNull { matcher ->
            matcher.keywords.any { k -> combined.contains(k) }
        }
    }

    private fun checkTailored(
        pkgName: String,
        entry: UadEntry?,
        matcher: BrandMatcher?,
        model: String?
    ): Pair<Boolean, String?> {
        if (matcher == null) return false to null
        val matchesPrefix = matcher.packagePrefixes.any { pkgName.startsWith(it) }
        val matchesDesc = entry?.description?.contains(matcher.displayName, ignoreCase = true) == true ||
            matcher.keywords.any { entry?.description?.contains(it, ignoreCase = true) == true }
        val matchesModel = !model.isNullOrBlank() && entry?.description?.contains(model, ignoreCase = true) == true

        if (matchesPrefix || matchesDesc || matchesModel) {
            val reason = if (matchesModel) "Specifically tailored for your $model"
                else "Tailored recommendation for ${matcher.displayName} devices"
            return true to reason
        }
        return false to null
    }

    /** Scan packages installed on the local device. */
    suspend fun scanLocalPackages(
        brand: String? = android.os.Build.BRAND,
        manufacturer: String? = android.os.Build.MANUFACTURER,
        model: String? = android.os.Build.MODEL
    ): List<DevicePackage> = withContext(Dispatchers.IO) {
        val cat = loadCatalog()
        val pm = context.packageManager
        val matcher = findBrandMatcher(brand, manufacturer, model)
        pm.getInstalledApplications(PackageManager.GET_META_DATA).map { appInfo ->
            toDevicePackage(appInfo.packageName, cat, pm, appInfo, matcher, model)
        }.sortedWith(
            compareBy<DevicePackage> { if (it.isTailored) 0 else 1 }
                .thenBy { it.tier.ordinal }
                .thenBy { it.packageName }
        )
    }

    /** Resolve categorized remote packages against UAD-ng or CleanYourTV catalog with real system/user/disabled status. */
    suspend fun resolveRemotePackages(
        categorized: CategorizedPackages,
        isTv: Boolean = false,
        brand: String? = null,
        manufacturer: String? = null,
        model: String? = null
    ): List<DevicePackage> =
        withContext(Dispatchers.IO) {
            val tvCat = if (isTv) loadTvCatalog() else emptyMap()
            val phoneCat = loadCatalog()
            val matcher = findBrandMatcher(brand, manufacturer, model)
            categorized.allPackages.map { pkgName ->
                val tvEntry = if (isTv) tvCat[pkgName] else null
                val phoneEntry = phoneCat[pkgName]
                val entry = tvEntry ?: phoneEntry
                val tier = entry?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
                val category = entry?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
                val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
                val description = tvEntry?.description ?: phoneEntry?.description
                    ?: if (isTv) "Not cataloged in CleanYourTV database." else "Not cataloged in UAD-ng database."

                val isSystem = pkgName in categorized.systemPackages || (pkgName !in categorized.userPackages)
                val isEnabled = pkgName !in categorized.disabledPackages
                val (isTailored, tailoredReason) = checkTailored(pkgName, entry, matcher, model)

                val appLabel = resolveAppLabel(pkgName, entry, isTv)

                DevicePackage(
                    packageName = pkgName,
                    appLabel = appLabel,
                    tier = tier,
                    category = category,
                    description = description,
                    isEnabled = isEnabled,
                    isSystemApp = isSystem,
                    isSafeguardLocked = isProtected,
                    safeguardReason = safeguardReason(pkgName, tier),
                    isTailored = isTailored,
                    tailoredReason = tailoredReason
                )
            }.sortedWith(
                compareBy<DevicePackage> { if (it.isTailored) 0 else 1 }
                    .thenBy { it.tier.ordinal }
                    .thenBy { it.packageName }
            )
        }

    /** Resolve a list of remote package names against UAD-ng or CleanYourTV catalog. */
    suspend fun resolveRemotePackages(
        names: List<String>,
        isTv: Boolean = false,
        brand: String? = null,
        manufacturer: String? = null,
        model: String? = null
    ): List<DevicePackage> =
        withContext(Dispatchers.IO) {
            val tvCat = if (isTv) loadTvCatalog() else emptyMap()
            val phoneCat = loadCatalog()
            val matcher = findBrandMatcher(brand, manufacturer, model)
            names.map { pkgName ->
                val tvEntry = if (isTv) tvCat[pkgName] else null
                val phoneEntry = phoneCat[pkgName]
                val entry = tvEntry ?: phoneEntry
                val tier = entry?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
                val category = entry?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
                val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
                val description = tvEntry?.description ?: phoneEntry?.description
                    ?: if (isTv) "Not cataloged in CleanYourTV database." else "Not cataloged in UAD-ng database."
                val (isTailored, tailoredReason) = checkTailored(pkgName, entry, matcher, model)
                val appLabel = resolveAppLabel(pkgName, entry, isTv)
                DevicePackage(
                    packageName = pkgName,
                    appLabel = appLabel,
                    tier = tier,
                    category = category,
                    description = description,
                    isEnabled = true,
                    isSystemApp = true,
                    isSafeguardLocked = isProtected,
                    safeguardReason = safeguardReason(pkgName, tier),
                    isTailored = isTailored,
                    tailoredReason = tailoredReason
                )
            }.sortedWith(
                compareBy<DevicePackage> { if (it.isTailored) 0 else 1 }
                    .thenBy { it.tier.ordinal }
                    .thenBy { it.packageName }
            )
        }

    private fun toDevicePackage(
        pkgName: String, cat: Map<String, UadEntry>,
        pm: PackageManager, appInfo: ApplicationInfo,
        matcher: BrandMatcher? = null,
        model: String? = null
    ): DevicePackage {
        val uad = cat[pkgName]
        val tier = uad?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
        val category = uad?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
        val (isTailored, tailoredReason) = checkTailored(pkgName, uad, matcher, model)
        return DevicePackage(
            packageName = pkgName,
            appLabel = pm.getApplicationLabel(appInfo).toString(),
            tier = tier,
            category = category,
            description = uad?.description ?: "Not cataloged in UAD-ng database.",
            isEnabled = appInfo.enabled,
            isSystemApp = isSystem,
            isSafeguardLocked = isProtected,
            safeguardReason = safeguardReason(pkgName, tier),
            isTailored = isTailored,
            tailoredReason = tailoredReason
        )
    }

    private fun safeguardReason(pkgName: String, tier: RemovalTier): String? = when {
        pkgName in PROTECTED_PACKAGES -> "Protected core system package"
        tier == RemovalTier.UNSAFE -> "Classified Unsafe by UAD-ng — high bootloop risk"
        else -> null
    }

    /** Resolve clean, human-readable application label for packages across local and remote devices */
    fun resolveAppLabel(pkgName: String, entry: UadEntry?, isTv: Boolean = false): String {
        // 1. Try local PackageManager if installed on host device
        val localLabel = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(pkgName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull()
        if (!localLabel.isNullOrBlank() && localLabel != pkgName) {
            return localLabel
        }

        // 2. Check well-known package dictionary
        KNOWN_APP_LABELS[pkgName]?.let { return it }

        // 3. Extract title from UAD-ng / CleanYourTV description if present
        entry?.description?.let { desc ->
            val clean = desc.trim()
            if (clean.isNotBlank() &&
                !clean.startsWith("Not cataloged", ignoreCase = true) &&
                !clean.startsWith("(CleanYourTV", ignoreCase = true)
            ) {
                val firstDelim = clean.indexOfAny(charArrayOf('.', ':', '—', '-'))
                if (firstDelim in 3..36) {
                    val candidate = clean.substring(0, firstDelim).trim()
                    val invalidStarters = listOf(
                        "this", "the", "a", "an", "used", "disabling", "pre-installed",
                        "preinstalled", "system", "allows", "provides", "required", "enables",
                        "tracks", "collects", "manages", "controls", "contains", "default",
                        "stock", "official", "background", "internal", "framework", "core",
                        "service", "daemon", "component", "handles", "helper", "implements",
                        "stores", "supports", "syncs", "can be", "safe to", "not needed",
                        "needed for", "part of", "telemetry", "analytics", "bloatware"
                    )
                    val candidateLower = candidate.lowercase()
                    val isSentence = invalidStarters.any { candidateLower.startsWith(it) } || candidate.split(' ').size > 5
                    if (!isSentence) {
                        return candidate
                    }
                }
            }
        }

        // 4. Parse friendly title from package segments with brand awareness
        return formatFriendlyPackageName(pkgName)
    }

    private val ACRONYMS = mapOf(
        "vlc" to "VLC", "mx" to "MX", "tv" to "TV", "ui" to "UI", "fx" to "FX", "cx" to "CX",
        "vpn" to "VPN", "dns" to "DNS", "ftp" to "FTP", "smb" to "SMB", "ssh" to "SSH",
        "ai" to "AI", "hd" to "HD", "qr" to "QR", "vr" to "VR", "ar" to "AR", "gps" to "GPS",
        "nfc" to "NFC", "sms" to "SMS", "mms" to "MMS", "voip" to "VoIP", "api" to "API",
        "sdk" to "SDK", "adb" to "ADB", "rar" to "RAR", "zip" to "ZIP", "pdf" to "PDF",
        "apk" to "APK", "mp3" to "MP3", "mp4" to "MP4", "mkv" to "MKV", "flac" to "FLAC",
        "oem" to "OEM", "aosp" to "AOSP", "ota" to "OTA", "fm" to "FM", "am" to "AM",
        "pc" to "PC", "lan" to "LAN", "wan" to "WAN", "bbk" to "BBK", "wifi" to "Wi-Fi"
    )

    private val COMPOUND_SUFFIXES = listOf(
        "explorer", "player", "manager", "recorder", "browser", "viewer", "launcher",
        "scanner", "cleaner", "booster", "keyboard", "battery", "gallery", "camera",
        "weather", "clock", "calculator", "calendar", "contacts", "dialer", "messages",
        "photos", "videos", "music", "audio", "stream", "remote", "media", "editor",
        "reader", "maker", "tracker", "connect", "master", "speedtest", "gamepass",
        "link", "guard", "warden", "casts", "pod", "arch", "station"
    )

    private fun formatFriendlyPackageName(pkgName: String): String {
        val parts = pkgName.split('.').filter { it.isNotBlank() }
        if (parts.isEmpty()) return pkgName

        // Detect brand prefix
        val brandPrefix = when {
            pkgName.startsWith("com.samsung.") || pkgName.startsWith("com.sec.") -> "Samsung"
            pkgName.startsWith("com.miui.") || pkgName.startsWith("com.xiaomi.") -> "Xiaomi"
            pkgName.startsWith("com.google.android.") || pkgName.startsWith("com.google.") -> "Google"
            pkgName.startsWith("com.oneplus.") -> "OnePlus"
            pkgName.startsWith("com.oppo.") || pkgName.startsWith("com.coloros.") || pkgName.startsWith("com.oplus.") -> "OPPO"
            pkgName.startsWith("com.vivo.") || pkgName.startsWith("com.bbk.") || pkgName.startsWith("com.iqoo.") -> "Vivo"
            pkgName.startsWith("com.sony.") || pkgName.startsWith("com.sonymobile.") -> "Sony"
            pkgName.startsWith("com.motorola.") || pkgName.startsWith("com.moto.") -> "Motorola"
            pkgName.startsWith("com.tcl.") -> "TCL"
            pkgName.startsWith("com.amazon.") -> "Amazon"
            pkgName.startsWith("com.huawei.") || pkgName.startsWith("com.hihonor.") -> "Huawei"
            pkgName.startsWith("com.transsion.") -> "Transsion"
            pkgName.startsWith("com.microsoft.") -> "Microsoft"
            else -> null
        }

        val ignored = setOf("com", "org", "net", "io", "ai", "android", "apps", "app", "vendor", "system", "sec", "miui", "xiaomi", "samsung", "google", "oppo", "vivo", "oneplus", "sony", "motorola", "tcl", "amazon", "microsoft")
        val meaningful = parts.filter { it.lowercase() !in ignored }

        // Suffixes that are generic distribution variants or internal codenames
        val genericSuffixes = setOf(
            "ad", "ads", "pro", "plus", "premium", "lite", "free", "trial", "beta", "alpha",
            "nightly", "dev", "canary", "preview", "release", "full", "mobile", "arm", "x86",
            "global", "intl", "row", "livingroom", "thirdpartyclient", "flutter", "mediaclient",
            "frontpage", "katana", "orca", "pop"
        )

        val target = when {
            meaningful.isEmpty() -> parts.last()
            meaningful.size >= 2 && meaningful.last().lowercase() in genericSuffixes -> {
                val core = meaningful[meaningful.size - 2]
                val suffix = meaningful.last().lowercase()
                if (suffix in setOf("pro", "plus", "premium", "lite", "beta", "alpha")) {
                    "$core $suffix"
                } else {
                    core
                }
            }
            meaningful.size >= 2 && (meaningful.last().lowercase() in setOf("tv", "ui", "service", "helper", "agent", "core", "client", "music", "browser", "video", "photos", "mail", "shopping")) -> {
                "${meaningful[meaningful.size - 2]} ${meaningful.last()}"
            }
            else -> meaningful.last()
        }

        // Title-case camelCase, underscores, and dashes
        val separated = target.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')

        // Compound lowercase word splitting
        val tokens = separated.split(' ').filter { it.isNotBlank() }
        val splitTokens = tokens.flatMap { token ->
            val lower = token.lowercase()
            val matchedSuffix = COMPOUND_SUFFIXES.firstOrNull { suffix ->
                lower.endsWith(suffix) && lower.length >= suffix.length + 3
            }
            if (matchedSuffix != null) {
                val prefix = token.substring(0, token.length - matchedSuffix.length)
                listOf(prefix, matchedSuffix)
            } else {
                listOf(token)
            }
        }

        val words = splitTokens.map { token ->
            val lower = token.lowercase()
            ACRONYMS[lower] ?: token.replaceFirstChar { c -> c.uppercase() }
        }

        val formattedTarget = words.joinToString(" ")
        return if (brandPrefix != null && !formattedTarget.startsWith(brandPrefix, ignoreCase = true)) {
            "$brandPrefix $formattedTarget"
        } else {
            formattedTarget
        }
    }
}
