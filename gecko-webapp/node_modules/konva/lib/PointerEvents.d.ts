import type { KonvaEventObject } from './Node.ts';
import type { Shape } from './Shape.ts';
import type { Stage } from './Stage.ts';
export interface KonvaPointerEvent extends KonvaEventObject<PointerEvent> {
    pointerId: number;
}
export declare function getCapturedShape(pointerId: number): Shape<import("./Shape.ts").ShapeConfig> | Stage | undefined;
export declare function createEvent(evt: PointerEvent): KonvaPointerEvent;
export declare function hasPointerCapture(pointerId: number, shape: Shape | Stage): boolean;
export declare function setPointerCapture(pointerId: number, shape: Shape | Stage): void;
export declare function releaseCapture(pointerId: number, target?: Shape | Stage): void;
