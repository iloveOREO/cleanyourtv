# Netflix remote pointer (Android accessibility service)

A small independent companion used to navigate the original mobile/tablet Netflix
app on one Android TV. This is not a Netflix app modification, a launcher module
card, or a certification/DRM bypass. The original Netflix client is not distributed.
The default target is only `com.netflix.mediaclient`; this does not operate the
TV client `com.netflix.ninja` or arbitrary apps.

## Build

From this fork's root, JDK 17 and Android SDK 34 installed:

```sh
./launcher/gradlew -p companions/netflix-remote --no-daemon lintRelease assembleRelease
```

Minimum Android is 8.0/API 26; only Android 11 on the TCL/FFALCON `tcl_mt5879_cn`
platform was used in the session. Build output:
`companions/netflix-remote/app/build/outputs/apk/release/app-release.apk`.
No APK, signing key or dependency binaries are added by this companion project.

## Permissions, service and data

The manifest declares an Android accessibility service guarded by
`android.permission.BIND_ACCESSIBILITY_SERVICE`. Its accessibility capabilities:
read the active window/node tree to find a click target, receive remote key events,
and dispatch a fallback gesture. It draws a non-focusable, non-touchable trusted
`TYPE_ACCESSIBILITY_OVERLAY`, rather than a system-alert overlay requiring a
separate draw-over-apps grant. There is no Internet, storage, root, microphone,
camera or account permission. There is no telemetry or persistent account data.

The service runs while enabled in Android accessibility settings. It checks the
foreground application, shows the cursor only in mobile Netflix, and passes Home,
source, volume and other unhandled keys through. Existing accessibility services
must remain enabled. The service logs only lifecycle/mode/click-result messages,
never node labels, entered text, account values or key sequences.

## Install and enable

1. Use the TV's normal supported install mechanism. If ADB installs are allowed,
   `adb -s "${TV_ADB_SERIAL:?Set the authorized device serial}" install -r companions/netflix-remote/app/build/outputs/apk/release/app-release.apk`.
   A verification rejection is a stop condition; this project supplies no bypass.
2. Prefer the TV's accessibility settings to enable **Netflix 遥控鼠标**.
3. On a TV without that settings screen, use the following small Python snippet
   through the existing authorized ADB connection. It appends one exact service,
   leaves the existing services intact, and writes a private local state snapshot
   exclusively (it refuses to overwrite an earlier record). Set `TV_PRIVATE_STATE`
   to an absolute private backup path outside any Git checkout before running.

```python
import json, os, shlex, subprocess
from pathlib import Path

serial = os.environ['TV_ADB_SERIAL']
adb = ['adb', '-s', serial, 'shell']
def shell(command):
    return subprocess.check_output(adb + [command], text=True, timeout=20).strip()

service = 'local.tv.netflixremote/local.tv.netflixremote.RemoteService'
original = shell('settings get secure enabled_accessibility_services')
enabled = shell('settings get secure accessibility_enabled')
# Keep this record private and outside the repository. Choose your own private path.
backup = Path(os.environ['TV_PRIVATE_STATE']).expanduser().resolve()
if any((parent / '.git').exists() for parent in (backup.parent, *backup.parents)):
    raise ValueError('Choose a private backup path outside every Git checkout')
with backup.open('x', opener=lambda path, flags: os.open(path, flags, 0o600)) as out:
    json.dump({'serial': serial, 'services': original, 'enabled': enabled}, out)
services = [] if original in ('', 'null') else original.split(':')
if service not in services:
    services.append(service)
shell('settings put secure enabled_accessibility_services ' + shlex.quote(':'.join(services)))
shell('settings put secure accessibility_enabled 1')
```

Some TCL firmware separately gates background starts with `APP_AUTO_START` and
`APP_ASSOC_START`. Inspect and record their current modes before deciding whether
to allow them for this service. The session allowed those two operations, but that
is not an instruction to grant every app broader autostart rights. Restore each
recorded mode when undoing. No reboot is required for the initial pointer test.

## Physical controls

- Open mobile Netflix: a white crosshair appears automatically.
- Direction keys move the pointer; hold a direction to accelerate.
- Short OK/Enter clicks the target, first through `ACTION_CLICK` without requiring
  pre-existing focus. A fallback touch temporarily removes the overlay.
- Menu toggles pointer mode **inside Netflix**. Outside Netflix, keys pass through.
- Home and source leave normally; Back and volume remain native.

Test with the actual remote; injected ADB keys are not sufficient evidence. Try one
click at a time. Pointer debounce ignores a second click during a fallback gesture,
and the delayed gesture rechecks the target before dispatch. IME, scrolling,
long-presses and every playback control have not been fully validated.

## Disable / restore

Disable **Netflix 遥控鼠标** in accessibility settings, or remove only its exact
component from the *current* colon-separated setting over ADB. Keep all other
services, including ones enabled after this helper; do not blindly overwrite the
whole setting with an old backup. If the result is empty and the original was
absent, delete the setting rather than writing the literal `null` as a service.
Do not change global `accessibility_enabled` while other services still need it.
Restore the originally absent/value state only after checking those services.

Then `adb -s "${TV_ADB_SERIAL:?Set the authorized device serial}" shell am force-stop local.tv.netflixremote`
removes its process/overlay. Restore any changed AppOps to their recorded modes.
This affects neither the launcher nor Netflix's account data. The private snapshot
is a recovery aid, not material for a public issue.

## Evidence and limits

The original session implementation was confirmed on a physical TCL/FFALCON remote:
move cursor, click Netflix's login button, enter the login page. The public revision
adds debounce and a late foreground recheck, validated by compilation and lint;
those small changes have not had a second physical-remote acceptance run. Account
login, actual streaming video/audio, picture quality and long-term stability remain
unverified. The helper cannot supply Netflix device certification.

Troubleshooting was informed by the public MATVT 1.0.6 project. This is an independent
implementation using Android accessibility APIs, not a redistribution of MATVT.
See [MIT licence](LICENSE). No Netflix or MATVT application is bundled.
