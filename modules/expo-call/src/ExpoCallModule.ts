import { NativeModule, requireNativeModule } from 'expo';
import type { CallState, ExpoCallModuleEvents } from './ExpoCall.types';

declare class ExpoCallModule extends NativeModule<ExpoCallModuleEvents> {
  getCallState(): CallState;
}

export default requireNativeModule<ExpoCallModule>('ExpoCall');
