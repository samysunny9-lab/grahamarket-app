# GrahaMarket 🪐📈

An **educational** Android app that computes the real, current positions of the nine
*grahas* (Vedic planets) and turns them into a 30-day "outlook" for Indian (NSE/BSE)
stocks, shown next to the stock's live price.

> ⚠️ **Educational only — not investment advice.**
> There is **no scientifically proven causal link** between planetary positions and
> stock prices. The percentage figures are a transparent, deterministic rule engine
> built for *learning about jyotish concepts and Android development* — not a validated
> predictor. Every screen carries this disclaimer. Please do not make financial
> decisions based on this app.

## What it does

- **Outlook tab** — type an NSE symbol (e.g. `RELIANCE`, `TCS`, `INFY`), see:
  - the **live price** (fetched from a configurable provider),
  - **7-day and 30-day** direction + percentage headlines,
  - a **day-by-day prediction** for each day of the horizon (toggle 7/30), where
    every day is scored from *that day's* real sidereal positions (the day's Moon
    sign/nakshatra is shown as the fastest-moving driver),
  - the **contributing graha factors**, and
  - an **"Add to Watchlist"** button.
- **Numerology tab** — the *same* 7 & 30-day prediction computed via **Chaldean
  numerology** instead of grahas: the symbol's name number, the date number, their
  compound, planetary rulerships, and a day-by-day series.
- **Watchlist tab** — stocks you saved, **persisted on-device** (survives app
  restarts). Each row shows the **date added**, **price when added**, and the
  **current live price** (refreshed on open / via Refresh), with a **Remove** button.
- **Deeper + Check tab** — two things on one screen:
  - **Deeper Jyotish (B):** a more authentic Vedic forecast using **Vimshottari
    Dasha** (running Mahadasha/Antardasha), **transit-to-natal** analysis and
    **bhava (house)** weighting from the stock's inferred natal Moon.
  - **Reality Check (A):** an honest **backtest** of all three engines (graha,
    numerology, deeper) showing their real **directional hit-rate** and
    **correlation** with market moves. Spoiler: hit-rate ≈ 50% and correlation ≈ 0,
    because astrology has no causal link to prices. This is the educational payoff.
- **Sky tab** — the live sidereal positions of all nine grahas: rashi, nakshatra,
  retrograde, dignity (exaltation/debilitation/own-sign) and combustion.
- **Settings tab** — switch the **price data provider** at runtime and store an
  API key if the chosen provider needs one.

Everything carries the **Low confidence / educational** label. Day-by-day figures
are a genuine computation from each day's real positions — not a fabricated forecast.

### Why the predictions "fail" — and why that's the point

The forecasts do not reliably predict the market, and no tuning can make them.
Planetary positions and numerology have **no proven causal relationship** with
stock prices. Rather than hide this, the **Reality Check** tab measures it
directly: it backtests each engine and shows the hit-rate landing near a coin-flip
(~50%) and correlation near zero. The app's value is teaching *why* such
predictors can't work — demonstrated with real statistics — not pretending they do.

### Vimshottari Dasha (authentic)

The dasha engine follows Brihat Parashara Hora Shastra: a 120-year cycle with
fixed lord periods (Ketu 7, Venus 20, Sun 6, Moon 10, Mars 7, Rahu 18, Jupiter 16,
Saturn 19, Mercury 17), started from the Moon's birth nakshatra, with the 27
nakshatras cycling the 9 lords three times. The balance of the opening dasha is
derived from the Moon's progress through its nakshatra. (Verified: periods sum to
120, nakshatra→lord mapping matches BPHS.)

## The astronomy (the serious part)

Positions are computed **on-device, offline**, using standard low-precision
Keplerian elements (Schlyter/Meeus) plus the dominant lunar perturbation terms,
then converted from tropical to **sidereal using the Lahiri (Chitrapaksha)
ayanamsa** — the standard for Vedic astrology.

The engine was cross-checked against published Vedic ephemerides for 2026 and
matches reality for the slow movers:

| Graha | GrahaMarket | Reference (2026) |
|-------|-------------|------------------|
| Saturn | Pisces (Meena) | Pisces all year |
| Jupiter | Cancer (Karka), **exalted** | Cancer, exalted (Jun–Oct) |
| Rahu | Aquarius (Kumbha) | Aquarius until late Nov |
| Ketu | Leo (Simha) | Leo until Dec 5 |

Derived features validated by the test harness:
- Rahu/Ketu always exactly 180° apart
- Nodes always retrograde; Sun/Moon never retrograde
- Sidereal new year: Sun ≈ 0° Mesha on ~14 April ✔

## The scoring engine

Each graha carries a benefic/malefic weight. The **benefic and malefic totals are
deliberately balanced (±4.2)** so direction comes from *current conditions*, not a
baked-in bias:

- **Dignity** — exaltation strengthens, debilitation weakens/reverses.
- **Retrograde (vakri)** — intensifies a graha's nature (×1.3), does not flip it.
- **Combustion (astam)** — halves a graha's expressed strength.
- **Per-stock "natal lord"** — a stable graha derived from the symbol's hash that is
  strongly amplified, so different stocks respond differently to the same sky.
- **Per-stock mood bias** — a stable offset so stocks aren't uniformly bullish.

The net score maps to a bounded estimate in **−18%…+18%** with a direction label.
It is **deterministic** (same symbol + same day → same result) and always reported
with **Low confidence**.

## Pluggable price providers

Live prices come through a `PriceProvider` interface so providers are swappable from
the Settings menu with zero code changes at call sites:

| Provider | Key required | Notes |
|----------|-------------|-------|
| **Yahoo Finance** (default) | No | Unofficial public endpoint. NSE = `.NS`, BSE = `.BO`. |
| **Twelve Data** | Yes (free) | Good NSE coverage. |
| **Finnhub** | Yes (free) | US-centric; alternate. |

The chosen provider and its key are persisted with Jetpack **DataStore**.

## Tech stack

- Kotlin, **Jetpack Compose** + Material 3 (dark "cosmic" theme)
- MVVM (`AndroidViewModel` + `StateFlow`)
- OkHttp + coroutines for networking
- DataStore Preferences for settings
- No native libraries — the ephemeris is pure Kotlin

## Build & run

Requires **Android Studio** (the Android SDK is needed to build an APK).

```bash
# from the project root
./gradlew assembleDebug      # build a debug APK
# or just open the folder in Android Studio and hit Run.
```

Minimum SDK 24, target/compile SDK 34.

## Project layout

```
app/src/main/java/com/grahamarket/
├── MainActivity.kt            # bottom-nav scaffold (Outlook / Sky / Settings)
├── astro/
│   ├── AstroMath.kt           # Julian day, angles, Lahiri ayanamsa
│   ├── Ephemeris.kt           # tropical longitudes (Sun, Moon, planets, nodes)
│   ├── Graha.kt               # enums, rashis, nakshatras, GrahaPosition
│   ├── GrahaEngine.kt         # dignity, retrograde, combustion → full positions
│   └── MarketScorer.kt        # graha states → 30-day outlook (educational)
├── data/
│   ├── PriceProvider.kt       # provider interface + types
│   ├── Providers.kt           # Yahoo / Twelve Data / Finnhub implementations
│   ├── SettingsStore.kt       # DataStore-backed provider + key persistence
│   └── MarketRepository.kt    # orchestrates quote + outlook
└── ui/
    ├── Theme.kt  Components.kt  HomeScreen.kt  GrahaScreen.kt  SettingsScreen.kt
    └── MainViewModel.kt
```

## License / disclaimer

For educational and entertainment purposes only. The authors make no claim that the
output has any predictive validity for financial markets.
