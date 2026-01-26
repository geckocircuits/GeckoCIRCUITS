import type { ShapeConfig } from '../Shape.ts';
import { Shape } from '../Shape.ts';
import { Group } from '../Group.ts';
import type { Context } from '../Context.ts';
import type { ContainerConfig } from '../Container.ts';
import type { GetSet } from '../types.ts';
import type { Text } from './Text.ts';
export interface LabelConfig extends ContainerConfig {
}
declare const NONE = "none";
export declare class Label extends Group {
    constructor(config?: LabelConfig);
    getText(): Text;
    getTag(): Tag;
    _addListeners(text: any): void;
    getWidth(): number;
    getHeight(): number;
    _sync(): void;
}
export interface TagConfig extends ShapeConfig {
    pointerDirection?: string;
    pointerWidth?: number;
    pointerHeight?: number;
    cornerRadius?: number | Array<number>;
}
export declare class Tag extends Shape<TagConfig> {
    _sceneFunc(context: Context): void;
    getSelfRect(): {
        x: number;
        y: number;
        width: number;
        height: number;
    };
    pointerDirection: GetSet<'left' | 'up' | 'right' | 'down' | typeof NONE, this>;
    pointerWidth: GetSet<number, this>;
    pointerHeight: GetSet<number, this>;
    cornerRadius: GetSet<number, this>;
}
export {};
