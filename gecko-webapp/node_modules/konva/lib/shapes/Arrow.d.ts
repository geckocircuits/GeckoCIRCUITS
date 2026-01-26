import type { LineConfig } from './Line.ts';
import { Line } from './Line.ts';
import type { GetSet } from '../types.ts';
import type { Context } from '../Context.ts';
export interface ArrowConfig extends LineConfig {
    points: number[];
    tension?: number;
    closed?: boolean;
    pointerLength?: number;
    pointerWidth?: number;
    pointerAtBeginning?: boolean;
    pointerAtEnding?: boolean;
}
export declare class Arrow extends Line<ArrowConfig> {
    _sceneFunc(ctx: Context): void;
    __fillStroke(ctx: Context): void;
    getSelfRect(): {
        x: number;
        y: number;
        width: number;
        height: number;
    };
    pointerLength: GetSet<number, this>;
    pointerWidth: GetSet<number, this>;
    pointerAtEnding: GetSet<boolean, this>;
    pointerAtBeginning: GetSet<boolean, this>;
}
