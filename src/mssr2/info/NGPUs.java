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
package mssr2.info;

import net.haesleinhuepf.clij.CLIJ;

/**
 *
 * @author raul_
 */
public class NGPUs extends Thread{
    @Override
    public void run(){
//        System.out.println(CLIJ.clinfo());
//                    System.out.println("In development");
        CLIJ.getAvailableDeviceNames().stream().map(deviceName -> { return deviceName; }).forEachOrdered(deviceName -> {
            mssr2.core.MSSR.GPUsNames.add(deviceName);
//            if (deviceName.toLowerCase().contains("uhd") || deviceName.toLowerCase().contains("gfx") ||
//                    deviceName.toLowerCase().contains("asus") || deviceName.toLowerCase().contains("intel") ||
//                    deviceName.toLowerCase().contains("nvidia") || deviceName.toLowerCase().contains("geforce") ||
//                    deviceName.toLowerCase().contains("Quadro")) {
////                System.out.println("Encontre: " + deviceName);
//                mssr2.core.MSSR.GPUsNames.add(deviceName);
//            } else{
//                System.out.println("Not in my list to add: " + deviceName);
//            }
        });
//        System.out.println("In development");
        if (mssr2.core.MSSR.GPUsNames.size() < 1) {
            mssr2.core.MSSR.GPUsNames.add("No GPUs");
//            mssr.core.MSSR.GPUsNames.add("");
        }
    }
}
