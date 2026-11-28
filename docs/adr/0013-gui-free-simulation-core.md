# GUI-free simulation core

Date: 2026-02-12

The Swing desktop stays the primary tool for interactive work, and the REST API is added beside it rather than replacing it.
Both run the same numerical code, which lives in `gecko-simulation-core` and may not import AWT or Swing.
`CorePackageValidationTest` fails the build on such an import, and the REST module's Maven enforcer bans GUI dependencies.

Computation classes that used to call desktop singletons now depend on a small interface, such as `LossFileAccessor` or `ExternalStorageConverter`.
The desktop supplies an adapter that delegates to its windows and dialogs, while headless callers and tests supply their own.
The default constructors keep using the desktop adapter, so existing desktop callers did not change.

Replacing the desktop with a browser UI was rejected because power users depend on its editor, oscilloscope and Simulink integration.

Related: [local simulation session](0007-local-simulation-session.md), [pull-based frontend](0010-pull-based-frontend-single-owner-session.md).
