import { useEffect, useState } from 'react';
import type { CallState } from '../../modules/expo-call/src/ExpoCall.types';
import ExpoCallModule from '../../modules/expo-call/src/ExpoCallModule';

export function useCall() {
  const [state, setState] = useState<CallState>(() =>
    ExpoCallModule.getCallState(),
  );

  useEffect(() => {
    const subscription = ExpoCallModule.addListener(
      'onCallStateChange',
      ({ state }) => setState(state),
    );

    return () => subscription.remove();
  }, []);

  return state;
}
