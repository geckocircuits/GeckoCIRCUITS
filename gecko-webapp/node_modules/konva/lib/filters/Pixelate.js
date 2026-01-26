import { Factory } from "../Factory.js";
import { Util } from "../Util.js";
import { Node } from "../Node.js";
import { getNumberValidator } from "../Validators.js";
export const Pixelate = function (imageData) {
    let pixelSize = Math.ceil(this.pixelSize()), width = imageData.width, height = imageData.height, nBinsX = Math.ceil(width / pixelSize), nBinsY = Math.ceil(height / pixelSize), data = imageData.data;
    if (pixelSize <= 0) {
        Util.error('pixelSize value can not be <= 0');
        return;
    }
    for (let xBin = 0; xBin < nBinsX; xBin += 1) {
        for (let yBin = 0; yBin < nBinsY; yBin += 1) {
            let red = 0;
            let green = 0;
            let blue = 0;
            let alpha = 0;
            const xBinStart = xBin * pixelSize;
            const xBinEnd = xBinStart + pixelSize;
            const yBinStart = yBin * pixelSize;
            const yBinEnd = yBinStart + pixelSize;
            let pixelsInBin = 0;
            for (let x = xBinStart; x < xBinEnd; x += 1) {
                if (x >= width) {
                    continue;
                }
                for (let y = yBinStart; y < yBinEnd; y += 1) {
                    if (y >= height) {
                        continue;
                    }
                    const i = (width * y + x) * 4;
                    red += data[i + 0];
                    green += data[i + 1];
                    blue += data[i + 2];
                    alpha += data[i + 3];
                    pixelsInBin += 1;
                }
            }
            red = red / pixelsInBin;
            green = green / pixelsInBin;
            blue = blue / pixelsInBin;
            alpha = alpha / pixelsInBin;
            for (let x = xBinStart; x < xBinEnd; x += 1) {
                if (x >= width) {
                    continue;
                }
                for (let y = yBinStart; y < yBinEnd; y += 1) {
                    if (y >= height) {
                        continue;
                    }
                    const i = (width * y + x) * 4;
                    data[i + 0] = red;
                    data[i + 1] = green;
                    data[i + 2] = blue;
                    data[i + 3] = alpha;
                }
            }
        }
    }
};
Factory.addGetterSetter(Node, 'pixelSize', 8, getNumberValidator(), Factory.afterSetFilter);
