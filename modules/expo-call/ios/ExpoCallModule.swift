import ExpoModulesCore
import CallKit

private class CallObserverDelegate: NSObject, CXCallObserverDelegate {
  private let onChange: () -> Void

  init(onChange: @escaping () -> Void) {
    self.onChange = onChange
  }

  func callObserver(_ callObserver: CXCallObserver, callChanged call: CXCall) {
    onChange()
  }
}

public class ExpoCallModule: Module {
  private let callObserver = CXCallObserver()
  private var delegate: CallObserverDelegate?
  private var lastState: String?

  private func currentState() -> String {
    let active = callObserver.calls.filter { !$0.hasEnded }

    if active.contains(where: { $0.hasConnected || $0.isOutgoing }) {
      return "active"
    }
    if !active.isEmpty {
      return "ringing"
    }
    return "idle"
  }

  private func emitIfChanged() {
    let state = currentState()
    if state != lastState {
      lastState = state
      sendEvent("onCallStateChange", ["state": state])
    }
  }

  public func definition() -> ModuleDefinition {
    Name("ExpoCall")

    Events("onCallStateChange")

    Function("getCallState") { () -> String in
      return self.currentState()
    }

    OnStartObserving {
      self.lastState = self.currentState()
      let observerDelegate = CallObserverDelegate { [weak self] in
        self?.emitIfChanged()
      }
      self.delegate = observerDelegate
      self.callObserver.setDelegate(observerDelegate, queue: nil)
    }

    OnStopObserving {
      self.callObserver.setDelegate(nil, queue: nil)
      self.delegate = nil
    }
  }
}
