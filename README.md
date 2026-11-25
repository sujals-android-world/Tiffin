# Tiffin (Android Developer Intern Assignment)

A single-module Kotlin and Jetpack Compose Android app (`minSdk 24`) for discovering nearby home kitchens, previewing daily and weekly tiffin menus, and unlocking full access through a persisted ₹1 trial, then ₹249/month, subscription flow (purchase is faked).

---

## What I cut and why

**Time spent:** about 5 hours on the app build. I kept scope tight so the core requirements (paywall persistence across rotation, process death and force-stop) work cleanly.

- **Search bar, cuisine chips, veg/non-veg toggle, sorting:** cut to avoid complex multi-filter state and to keep focus on persistence. I added a single compact max-budget (₹) filter in the top bar instead.
- **Full weekly menu for free users:** free users see only today's menu; the other 6 days stay visible but locked.
- **Paywall on every launch after the 3rd:** the paywall opens automatically only once, on the 3rd cold launch after install. It still opens on every "Subscribe" tap until the user has paid.
- **Retry button on the error state:** cut because the ViewModel uses a one-shot flow to read bundled local data.
- **Food photos and heavy animations:** skipped to keep the APK light and scrolling smooth on older devices.

## What I added and why

- **One-time paywall on the 3rd launch:** repeated popups hurt browsing. One well-timed nudge on the 3rd cold launch reaches returning users without annoying them. The paywall still opens on every "Subscribe" tap until the user has paid.
- **Today-only free preview with visible locked days:** seeing today's meal builds trust in the kitchen's food, and seeing the other 6 days visibly locked shows exactly what a subscription unlocks.
- **Max-budget filter instead of a search bar:** a small ₹ field in the top bar shows only kitchens whose price per tiffin is less than or equal to the amount entered. It lets users find kitchens that fit their daily budget without free-text search or a filter sheet.
- **Two unit test files:** `SubscriptionRepositoryTest` (paywall trigger and the today-only menu rule) and `KitchenRepositoryTest` (budget filter and kitchen lookup by id).

## Assumptions

Before building I emailed two questions: (1) should free users see a partial weekly menu, or the full menu with only delivery slots locked, and (2) should the paywall open only on the 3rd cold launch, or on every launch from the 3rd onward. I went ahead with these defaults instead of guessing silently:

1. Free users see today's menu only. The other days are locked.
2. Free users can see delivery slot timings (on the list and detail screens) so they can judge whether a kitchen suits them. Choosing or booking a slot is the paid feature and is not built. This is a deliberate choice, and it is a looser reading of "paid users get delivery slots".
3. The paywall opens automatically once, on the 3rd cold launch, and a "shown" flag in DataStore stops it from reopening after a force-stop. Every "Subscribe" tap opens it until the user has paid.
4. The purchase is faked but persisted. After "paid" the paywall never opens again. The paywall states that ₹1 is charged now and ₹249 is charged on a named date, 24 hours later.

## Architecture

Single module, Kotlin, Jetpack Compose, `minSdk 24`:

- `data/`: parses `assets/kitchens.json` (12 kitchens) and persists launch count, paywall-shown flag and paid status with Preferences DataStore.
- `domain/`: `Kitchen`, `KitchenListUiState` (`Loading`, `Success`, `Error`), `KitchenRepository` and `SubscriptionRepository`.
- `presentation/`: `KitchenListScreen`, `KitchenDetailScreen`, `PaywallScreen`, ViewModels and Navigation Compose.
- `Analytics.kt`: one interface and a Logcat implementation, called at four moments: `app_launch`, `subscribe_tapped`, `paywall_shown`, `purchase_completed`.
- `test/`: JUnit4 tests for the budget filter, kitchen lookup, paywall trigger and today-only menu rules. Known limitation: the rule logic is written inside the test files instead of being called from production code, so moving it into `domain/` is the first cleanup I would do.

## How to build and run

1. Clone the repository and open it in Android Studio (JDK 17).
2. Let Gradle sync finish (no API keys or local secrets are needed).
3. Run the `app` configuration on a device or emulator (API 24 or higher).
4. To build the debug APK from the command line:

   ```bash
   ./gradlew assembleDebug
   ```

   The APK is at `app/build/outputs/apk/debug/app-debug.apk`.
5. To run the unit tests:

   ```bash
   ./gradlew testDebugUnitTest
   ```

## How to verify persistence

- **Rotation:** rotate on each screen; UI state and scroll position stay.
- **Process death:** turn on "Don't keep activities" in Developer options, or run `adb shell am kill com.example.tiffin`, then reopen.
- **Force-stop:** Settings > Apps > Tiffin > Force stop, then reopen; launch count and paid status are still right.
- **Paywall:** cold-launch the app three times; the paywall opens on the 3rd launch. Tap Subscribe, complete the simulated purchase and reopen; the paywall does not come back.

---

## Task 2: Fix what the agent wrote

### Original code

```kotlin
@Composable
fun KitchenList(vm: KitchenViewModel) {
    var items = mutableListOf<Kitchen>()
    LaunchedEffect(items) {
        val res = URL(BASE + "?key=" + "sk_live_9f2c41ab")
            .readText()
        items.addAll(parse(res))
    }
    LazyColumn { items(items) { KitchenRow(it) } }
}
```

### The four problems

1. **Hardcoded live secret key (`sk_live_9f2c41ab`) in client code.** Anyone can pull it out of the APK or the Git history and run up usage on our bill. Sending it in the URL query string also exposes it in network and server logs.
2. **`mutableListOf` is not Compose state and is not remembered.** It is recreated on every recomposition, and `addAll` does not trigger a recomposition, so the user is stuck on an empty screen. This is the one-star review.
3. **`URL.readText()` runs on the main thread with no error handling.** On Android this throws `NetworkOnMainThreadException` and crashes the app, and there is no loading or error state for a failed request.
4. **`LaunchedEffect(items)` is keyed on a list that is new on every recomposition.** The effect restarts again and again and spams the paid API (another bill). Because the fetch is not in the ViewModel (`vm` is unused), every rotation fetches again too.

Smaller issue: `items(items)` has no stable `key`.

### Corrected version

The leaked key must be revoked and rotated now, and kept only on our own backend proxy, never inside the APK.

```kotlin
// data
class KitchenApi(private val baseUrl: String) {
    // baseUrl points to our backend proxy, which attaches the API key server-side
    suspend fun fetchKitchens(): List<Kitchen> = withContext(Dispatchers.IO) {
        val text = URL(baseUrl).openStream().bufferedReader().use { it.readText() }
        parse(text)
    }
}

// presentation
sealed interface KitchenListUiState {
    data object Loading : KitchenListUiState
    data class Success(val kitchens: List<Kitchen>) : KitchenListUiState
    data class Error(val message: String) : KitchenListUiState
}

class KitchenViewModel(private val api: KitchenApi) : ViewModel() {
    val uiState: StateFlow<KitchenListUiState> = flow<KitchenListUiState> {
        emit(KitchenListUiState.Loading)
        emit(KitchenListUiState.Success(api.fetchKitchens()))
    }
        .catch { e ->
            emit(KitchenListUiState.Error(e.localizedMessage ?: "Failed to load kitchens"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KitchenListUiState.Loading
        )
}

@Composable
fun KitchenList(vm: KitchenViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        KitchenListUiState.Loading -> CircularProgressIndicator()
        is KitchenListUiState.Error -> Text(s.message)
        is KitchenListUiState.Success -> LazyColumn {
            items(s.kitchens, key = { it.id }) { KitchenRow(it) }
        }
    }
}
```

What this fixes: no secret in the client (1); state comes from a `StateFlow`, so the UI recomposes when data arrives (2); the call runs on `Dispatchers.IO` and failures become an Error state (3); the fetch lives in the ViewModel, so it runs once and survives rotation (4).

---

## Task 3: Release note

**Tiffin 1.0 (Play Store)**

Browse home kitchens near you, see today's menu, and subscribe to unlock the full weekly menu. Start with a ₹1 trial; the ₹249/month plan activates automatically 24 hours later.

### What I would test by hand before publishing

- **Rotation and process death:** rotate on all three screens, and trigger process death (`adb shell am kill`, and "Don't keep activities"), including while the paywall is open.
- **Force-stop:** force-stop from App info and reopen; launch count, the one-time paywall flag and the paid state must persist.
- **Paywall rules:** it opens automatically only once, on the 3rd cold launch; it opens on every Subscribe tap until paid; it never opens after purchase; the ₹249 date is correct, also after changing the phone's date or time zone.
- **Install flows:** fresh install, upgrade over an older build, and the system back button from the paywall.
- **Devices:** a low-end Android 7 (API 24) phone with 2 GB RAM, a mid-range Android 13 or 14 phone, a Pixel on Android 14 or 15, a Xiaomi or Samsung phone (aggressive background killing, good for process death), and a tablet or foldable emulator for layout.

### The one thing most likely to break in production

The persisted launch count and paid flag getting out of sync: counting a rotation as a cold start, or losing DataStore state after an update or process death, so a paid user sees the paywall again.

**How I would find out without a user telling me:** log `paywall_shown` (with an `is_paid` property) and `purchase_completed` to a real analytics backend, alert when `paywall_shown` fires with `is_paid == true`, and watch Play Console vitals and Crashlytics for DataStore `IOException` spikes. The current build only logs to logcat, so this needs the analytics and Crashlytics swap first.

### If it broke at 11pm: rollback

1. **Halt the rollout:** pause the staged rollout in Play Console so users who have not updated do not get the bad build.
2. **Ship the previous good code again:** a published version cannot be pulled back from users who already have it, so release the last known-good code with a higher `versionCode` as a hotfix.
3. **Remote kill switch (not in 1.0):** put the paywall trigger behind a Firebase Remote Config flag so it can be switched off in minutes without store review. This would be the first thing in the next release.

### Next three things I would build, in order

1. **Google Play Billing:** replace the fake purchase with a real ₹1 trial then ₹249/month subscription, with server-side purchase verification.
2. **Observability:** remote analytics, Crashlytics and the Remote Config kill switch, so problems are visible without waiting for reviews.
3. **Paid features:** delivery slot booking, meal skip controls, and search with filters.

---

## Task 4: How I used AI

### Tools and share of code

I used Claude, Gemini and Android Studio AI for planning, the ViewModel and UI state design, Compose screen scaffolding, the 12-kitchen JSON asset and unit test boilerplate. Roughly 60% of the code started as AI output, and 40% was written or refactored by me (architecture wiring, Navigation Compose setup, DataStore persistence rules and layout refinements).

### Best prompt, and what I did with the result

> In my Jetpack Compose Tiffin app, add a small price filter to the top bar of the kitchen list screen. It should be a compact input where the user types a maximum budget in rupees. The list should show only kitchens whose pricePerTiffin is less than or equal to that number. Do not add a search bar, free-text search, cuisine chips or sorting. Keep it to this one filter. Loading, empty and error states must keep working, and if no kitchen matches the budget, show the empty state.


- **What I asked for:** a small price-only filter. The user enters a budget, and the list shows only kitchens whose price per tiffin is less than or equal to that amount. No search bar and no free-text search.
- **What the agent returned:** a full search feature (a search bar with extra filtering) instead of the price-only filter. That is the scope I had deliberately cut, and it would have added more filter state to test.
- **What I did:** I noticed it did not match what I asked for, repeated the requirement as a narrower request (price only, less than or equal), and kept only that filter. I then put it in the top bar as a compact field.

### What the agent got wrong in my build

- **Layout and gating:** the agent put the max-budget (₹) filter in a full-width row below the top bar, which wasted vertical space, and it locked delivery slots on the detail screen. I caught both while testing on a device, moved the filter into a compact pill inside the `TopAppBar`, and made slot timings visible so users can check them before subscribing (see Assumptions).
- **Unused state:** the agent generated `KitchenListUiState` with an `Idle` state that was never emitted, which left a dead branch in the UI `when`. I removed `Idle`, so the state now starts in `Loading`.

### What I wrote by hand because directing an agent was slower

Wiring the Navigation Compose routes, fine-tuning the dp padding and the side-by-side delivery slot alignment on `KitchenDetailScreen`, and adjusting the 24-hour renewal date formatting directly in Kotlin. Each was under a minute by hand, versus writing multi-line UI prompts.
