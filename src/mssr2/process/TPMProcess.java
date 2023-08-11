/*
 * Copyright (C) 2021 raul_
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mssr2.process;

import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import java.util.ArrayList;

/**
 *
 * @author raul_
 */
public class TPMProcess implements PlugIn{

    @Override
    public void run(String string) {
        ImagePlus imgBase = WindowManager.getCurrentImage();
        ImagePlus mssrdImage;
        int numImagesPerFrame = 100;
        int NS = imgBase.getNSlices();
        if(NS>100){
            ArrayList<ImagePlus> batchImages = CoreProcess.stack2stacks(imgBase, 100,
                    (int) Math.floor(NS/numImagesPerFrame),
                    NS % numImagesPerFrame);
            ImagePlus[] batchIp = batchImages.toArray(new ImagePlus[batchImages.size()]);
//            batchImages.toArray(batchIp);
            batchImages = null;
            mssrdImage = CoreProcess.TPMBatch(batchIp, imgBase.getWidth(), imgBase.getHeight());
            batchIp = null;
        } else{
            mssrdImage = CoreProcess.TPM(imgBase);
        }
        mssrdImage.setSlice(1);
        mssrdImage.setTitle("TPM_"+imgBase.getTitle());
        mssrdImage.resetDisplayRange();
        mssrdImage.show();
    }
    
}
