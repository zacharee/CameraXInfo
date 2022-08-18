# CameraX Info
A simple app to list supported Camera2/CameraX extensions and other camera capabilities.

Downloads are available on the [Releases page](https://github.com/zacharee/CameraXInfo/releases).

# Database
CameraX Info also has an online database feature, where users can anonymously upload, browse, and download camera support data for different devices and Android versions.

This database relies on Firebase Firestore as the backend for storing device data.

# Privacy
CameraX Info uses Firebase Crashlytics and Firebase Firestore. Crashlytics is used to collect crash logs and Firestore is used for storing uploaded camera data.

No personal data is collected and your camera data isn't uploaded unless you upload it yourself.

## Data

Data is uploaded as JSON, with a form similar to:

```
{
    "device_brand": "samsung",
    "device_model": "SM-S908U1",
    "device_sdk": 31,
    "device_release": "12",
    "device_security": "2022-07-01",
    "arcore": {
        "arcore_support": "SUPPORTED_INSTALLED",
        "depth_support": true
    },
    "0": {
        "lens_facing": "Rear-Facing",
        "fov": "90",
        "resolution": "4000x3000",
        "physical_sensors": [
            {
                "lens_facing": "Rear-Facing",
                "fov": "119",
                "resolution": "4000x3000"
            },
            {
                "lens_facing": "Rear-Facing",
                "fov": "90",
                "resolution": "4000x3000"
            },
            {
                "lens_facing": "Rear-Facing",
                "fov": "38",
                "resolution": "3648x2736"
            },
            {
                "lens_facing": "Rear-Facing",
                "fov": "11.4",
                "resolution": "3648x2736"
            }
        ],
        "video_qualities": [
            "480p",
            "720p",
            "1080p",
            "2160p"
        ],
        "extensions": {
            "Auto": {
                "camera2": true,
                "camerax": true
            },
            "Bokeh": {
                "camera2": true,
                "camerax": true
            },
            "HDR": {
                "camera2": true,
                "camerax": true
            },
            "Night": {
                "camera2": true,
                "camerax": true
            },
            "Face Retouch": {
                "camera2": true,
                "camerax": true
            }
        }
    },
    "1": {
        "lens_facing": "Front-Facing",
        "fov": "83.7",
        "resolution": "3648x2736",
        "physical_sensors": [],
        "video_qualities": [
            "480p",
            "720p",
            "1080p"
        ],
        "extensions": {
            "Auto": {
                "camera2": true,
                "camerax": true
            },
            "Bokeh": {
                "camera2": true,
                "camerax": true
            },
            "HDR": {
                "camera2": true,
                "camerax": true
            },
            "Night": {
                "camera2": true,
                "camerax": true
            },
            "Face Retouch": {
                "camera2": true,
                "camerax": true
            }
        }
    },
    "2": {
        "lens_facing": "Rear-Facing",
        "fov": "119",
        "resolution": "4000x3000",
        "physical_sensors": [],
        "video_qualities": [
            "480p",
            "720p",
            "1080p"
        ],
        "extensions": {
            "Auto": {
                "camera2": true,
                "camerax": true
            },
            "Bokeh": {
                "camera2": true,
                "camerax": true
            },
            "HDR": {
                "camera2": true,
                "camerax": true
            },
            "Night": {
                "camera2": true,
                "camerax": true
            },
            "Face Retouch": {
                "camera2": true,
                "camerax": true
            }
        }
    },
    "3": {
        "lens_facing": "Front-Facing",
        "fov": "71.7",
        "resolution": "3216x2208",
        "physical_sensors": [],
        "video_qualities": [
            "480p",
            "720p",
            "1080p"
        ],
        "extensions": {
            "Auto": {
                "camera2": true,
                "camerax": true
            },
            "Bokeh": {
                "camera2": true,
                "camerax": true
            },
            "HDR": {
                "camera2": true,
                "camerax": true
            },
            "Night": {
                "camera2": true,
                "camerax": true
            },
            "Face Retouch": {
                "camera2": true,
                "camerax": true
            }
        }
    }
}
```

There are some quirks with this format:
- `arcore.depth_support` may not be present (this means the support is in an unknown state).
- `fov` fields may have values with comma-separated decimals instead of period-separated decimals (e.g., `90,9` instead of `90.9`).
- `lens_facing` fields may have the value `Front Facing` instead of `Front-Facing`.

Other fields and values should be consistent.
