# Isolated electrical groups

Date: 2026-11-17

Wired circuits no longer need a ground node or a conductive path from every component to ground.
The backend references them like the desktop.
The first wire potential is the numerical datum (ADR-0011).
Every other conductively isolated group gets 1 S from its lowest numerical node to the datum.
The desktop does the same in `NetzlisteAllg.deSingularizeIsolatedPotentials` and `LKMatrices.schreibeMatrix_A`.

No current can leave an isolated group, so the stamp carries no current.
It only fixes the group's reference potential and changes no voltage difference or branch current.
Five education transformer circuits have no ground label and a galvanically isolated secondary.
Under this rule they prepare as Ready and match their unchanged legacy goldens within 1% on the core backend.

Label-only circuits have no desktop wire order, so they keep the `UNCONNECTED_GROUND` admission diagnostic.
Their existing INVALID contracts stay unchanged: a floating island, a missing ground, and nodes driven only by current sources.
Ideal current sources still do not connect a group, because they add no conductance.

Accepting islands only when they have a magnetic coupling to a grounded group was rejected.
The transformer examples have no ground at all, so that rule does not admit them.
Removing the diagnostic for every model was also rejected.
It would silently change the label-only admission contract, which has no desktop reference to preserve.

Core contracts cover the pinned island voltages with and without the desktop node order.
They also cover a wired divider without ground.
