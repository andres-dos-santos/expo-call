# Expo Call

`expo-call` is a local Expo native module that reports whether the device is
currently idle, ringing, or involved in an active call. It provides a
synchronous state snapshot and emits an event whenever the detected state
changes.

The module is intended for application behavior that needs to react to calls,
such as pausing media, reducing audio activity, hiding call-sensitive UI, or
recording an interruption in local application state. It observes calls only;
it does not place, answer, reject, or end them.

## Supported platforms

| Platform | Native source | Detection mechanism |
| --- | --- | --- |
| iOS | `ios/ExpoCallModule.swift` | CallKit's `CXCallObserver` |
| Android 12 and newer | `android/.../ExpoCallModule.kt` | `AudioManager.OnModeChangedListener` |
| Android 11 and older | `android/.../ExpoCallModule.kt` | `AudioManager` mode polling every second while observed |
| Web | Not supported | No web implementation is provided |

No runtime permission is declared by this module. In particular, it does not
request access to the call log, phone numbers, contacts, or microphone.

## Call states

The public `CallState` type contains three values:

| State | Meaning |
| --- | --- |
| `idle` | No call-related activity is currently detected. |
| `ringing` | An incoming call is present but has not connected yet, or Android reports ringtone mode. |
| `active` | A connected or outgoing call is present, or Android reports in-call/communication audio mode. |

State detection is based on the native signals available on each platform.
Consequently, the meaning is intentionally broad and is not a complete phone
call lifecycle model.

## Usage

### React hook

This project exposes `useCall` from `src/hooks/use-call.ts`. The hook reads the
current state immediately and subscribes to later changes:

```tsx
import { Text, View } from 'react-native';
import { useCall } from '@/hooks/use-call';

export function CallStatus() {
  const callState = useCall();

  return (
    <View>
      <Text>Call state: {callState}</Text>
    </View>
  );
}
```

The hook returns a `CallState` string. Its listener is removed automatically
when the consuming component unmounts.

An application can derive behavior directly from that value:

```tsx
const callState = useCall();
const shouldPausePlayback = callState !== 'idle';
```

### Native module API

The module can also be consumed directly when a hook is not appropriate:

```ts
import ExpoCallModule from './src/ExpoCallModule';

const initialState = ExpoCallModule.getCallState();

const subscription = ExpoCallModule.addListener(
  'onCallStateChange',
  ({ state }) => {
    console.log('Call state changed:', state);
  },
);

// Remove the listener when it is no longer needed.
subscription.remove();
```

Read the initial value with `getCallState()` before relying on events. Adding a
listener starts native observation but does not emit an initial event; events
are sent only for changes detected after observation begins.

## API reference

### `getCallState()`

```ts
getCallState(): CallState
```

Returns the state currently inferred by the native platform. The call is
synchronous and does not require a permission prompt.

### `onCallStateChange`

Subscribe through the Expo `NativeModule.addListener` API:

```ts
ExpoCallModule.addListener(
  'onCallStateChange',
  (event: CallStateChangeEvent) => void,
);
```

The event payload has the following shape:

```ts
type CallStateChangeEvent = {
  state: CallState;
};
```

The native observer is activated when JavaScript starts observing the event
and is released when observation stops. Duplicate consecutive states are not
emitted.

### Type definitions

```ts
type CallState = 'idle' | 'ringing' | 'active';

type CallStateChangeEvent = {
  state: CallState;
};

type ExpoCallModuleEvents = {
  onCallStateChange: (event: CallStateChangeEvent) => void;
};
```

## Platform behavior

### iOS

iOS uses `CXCallObserver` to inspect non-ended calls known to CallKit:

- a connected call is reported as `active`;
- an outgoing call is reported as `active`, including before connection;
- another non-ended call is reported as `ringing`;
- no non-ended calls results in `idle`.

The observer reports aggregate application state rather than identifying an
individual call. If multiple calls exist, the presence of any connected or
outgoing call takes precedence over `ringing`.

### Android

Android maps the system audio mode to call state:

- `MODE_IN_CALL` and `MODE_IN_COMMUNICATION` map to `active`;
- `MODE_RINGTONE` maps to `ringing`;
- every other audio mode maps to `idle`.

On Android 12 (API 31) and newer, the module listens for audio mode changes.
On older versions, it checks the mode once per second while at least one event
listener is active. `getCallState()` itself always reads the current audio mode
directly.

## Limitations and privacy

- The module reports only a coarse state. It does not expose phone numbers,
  caller identity, direction, duration, or a call identifier.
- Android's `MODE_IN_COMMUNICATION` may be used by VoIP or other real-time
  audio applications. An `active` result can therefore represent communication
  audio rather than a cellular call.
- Native platform behavior and other calling applications determine which
  calls are visible to the underlying APIs.
- Rapid state transitions on Android 11 and older can be missed between the
  one-second polling intervals.
- Observation occurs while JavaScript listeners are registered. This module
  does not provide background processing or persistence by itself.
- The API should not be treated as authoritative evidence for billing,
  emergency, compliance, or security decisions.

Because the module exposes no call metadata and declares no Android
permissions, it avoids collecting personally identifiable call information.
Applications consuming the state remain responsible for handling any derived
analytics or stored data according to their privacy requirements.

## Module structure

```text
expo-call/
├── android/
│   └── src/main/java/expo/modules/call/ExpoCallModule.kt
├── ios/
│   ├── ExpoCall.podspec
│   └── ExpoCallModule.swift
├── src/
│   ├── ExpoCall.types.ts
│   └── ExpoCallModule.ts
└── expo-module.config.json
```

- `ExpoCallModule.ts` defines the typed JavaScript interface and resolves the
  native module named `ExpoCall`.
- `ExpoCall.types.ts` contains the public state and event types.
- The Swift and Kotlin files implement platform-specific state detection and
  event delivery.
- `expo-module.config.json` registers both native implementations with Expo
  Modules autolinking.

## License

See [LICENSE](./LICENSE).
