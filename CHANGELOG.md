# Changelog

## version 0.0.13

- fix the Diyanet (`TURKEY`) method away from Turkey. Fajr and Isha were bounded by
  `SEVENTH_OF_THE_NIGHT` above 48°N, which Diyanet does not use; errors reached 68 minutes in
  northern Europe. Added `HighLatitudeRule.PROPORTIONAL_DEPRESSION`, which reproduces Diyanet's
  "takdir" from 44.5° latitude upwards — its own published threshold — and made `TURKEY` use it.
- add `CalculationParameters.interpolateDeclination`. Diyanet holds the sun's declination at its
  0h value instead of interpolating it to each event, as Astronomical Algorithms prescribes. The
  difference is proportional to how fast the declination is moving, so it peaks at the equinoxes
  and lands almost entirely on the evening times. Turning it off for the Diyanet methods makes
  sunrise, dhuhr, asr and maghrib exact to the minute and halves the Isha error. Defaults to
  `true`, so every other method is unchanged.
- add `CalculationMethod.TURKEY_EUROPE` for Diyanet's European calendars, which use an Isha
  angle of 16° rather than 17°.
- the takdir now takes three forms, split at latitudes that both fall out of the geometry.
  Between 44.5°N and 46°N it bounds the twilight *duration* rather than the depression.
  Between 46°N and 48.56°N — which is 90° − 18° − 23.44°, the latitude at which the 18° sign
  first fails at the solstice — the bound is placed in **mean** solar time: Diyanet does
  not apply the equation of time there, which is worth up to ten minutes around the solstice and
  is why its published times are symmetric about the solstice when the true twilight is not.
  Above 48.56°N the bound is an ordinary apparent time, as before.
- implement Diyanet's five-hour floor on the day and the night, the rule set at the 2021
  Uluslararası Namaz Vakitleri Kongresi. Where it binds, sunrise and maghrib sit 2h30m either side
  of Öğle (or 9h30m when it is the night that is floored). This defines them inside the polar
  circle where the sun may not cross the horizon at all: `TURKEY_EUROPE` previously **threw** at
  Tromsø and produced an Isha *before* maghrib at Oulu. Verified exactly, to the minute, on every
  floored day at Tromsø, Oulu, Trondheim, Umeå, Helsinki, Tampere and Bergen.
- where the sun never rises, asr falls back to dhuhr, as Diyanet publishes on exactly those days,
  and asr is kept within the floored day at extreme latitudes.
- add full-year Diyanet reference times for 15 locations across 9 countries to `Shared/Times`.

Accuracy against Diyanet's published 2026 calendars, mean error in minutes (worst case in
brackets), before → after:

| | Turkey, 12 cities | Europe, 23 cities |
| --- | --- | --- |
| Fajr | 0.11 (1) → 0.08 (1) | 10.60 (68) → **0.55 (7)** |
| Sunrise | 0.14 (1) → 0.09 (1) | 0.30 (7) → **0.10 (7)** |
| Dhuhr | 0.10 (1) → 0.10 (1) | 0.12 (1) → 0.12 (1) |
| Asr | 0.72 (2) → **0.17 (1)** | 1.16 (4) → **0.16 (1)** |
| Maghrib | 0.61 (2) → **0.16 (1)** | 0.98 (3) → **0.18 (1)** |
| Isha | 0.79 (2) → **0.19 (1)** | 11.48 (61) → **0.63 (8)** |

Every Turkish time is now within one minute. In Europe sunrise, dhuhr, asr and maghrib are within
one minute, and Fajr and Isha are within two minutes on 92% of days. The remaining spread is close
to Diyanet's own, which disagrees by up to 8 minutes between cities sharing a latitude inside the
estimated window while agreeing to 0.3 minutes outside it.

## version 0.0.12

- update Kotlin to 2.3.21

## version 0.0.11

- fix for an edge case around international date line (thanks @z3bi)
- update Kotlin to 2.4.0

## version 0.0.8

- add polarCircleResolution to CalculationParameters

## version 0.0.7

- add maghribAngle to CalculationParameters

## version 0.0.6

- update Kotlin to 2.2.20
- update kotlinx-datetime (thanks @fathonyfath)
- support wasm target
- use vanniktech Gradle Maven Publish Plugin

## version 0.0.5

- update Kotlin to 1.9.22
- new target: Linux arm64
- support Turkish Diyanet method
- support Shafaq parameter
- support rounding, and update Singapore method to use up rounding
- update test data to [d418fb3](https://github.com/batoulapps/adhan-testdata/commit/d418fb37b3d011af5594e344c06c0e5616db2a5c).
- add a plethora of tests

## version 0.0.4

- support macOS arm64, Linux X64, Windows X64, JS, and watchOS
- update to Kotlin 1.7.10
- update kotlinx-datetime to 0.4.0

## version 0.0.3

- update to Kotlin 1.6.10
- support for Apple Silicon simulators

## version 0.0.2

- update to Kotlin 1.5.21
- rename package to com.batoulapps.adhan2 to allow coexisting with adhan

## version 0.0.1

- initial release using KMP
