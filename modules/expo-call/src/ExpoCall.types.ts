export type CallState = 'idle' | 'ringing' | 'active';

export type CallStateChangeEvent = {
  state: CallState;
};

export type ExpoCallModuleEvents = {
  onCallStateChange: (event: CallStateChangeEvent) => void;
};
