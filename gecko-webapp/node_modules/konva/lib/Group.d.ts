import type { ContainerConfig } from './Container.ts';
import { Container } from './Container.ts';
import type { Node } from './Node.ts';
import type { Shape } from './Shape.ts';
export interface GroupConfig extends ContainerConfig {
}
export declare class Group extends Container<Group | Shape> {
    _validateAdd(child: Node): void;
}
