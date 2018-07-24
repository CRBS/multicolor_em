# multicolor_em

[![DOI](https://zenodo.org/badge/142191150.svg)](https://zenodo.org/badge/latestdoi/142191150)


ImageJ plugin to overlay color-coded nanoSIMS/EELS chemical maps over the Conventional EM images


1. The Pre-requisite is to have all the images already opened in imageJ/Fiji. All the EELS images should be pre-aligned to the conventional image (using photoshop or any other tool) and they should be greyscale. The can be any format that is readable by Fiji i.e. Tiff, dm3 etc…and can be 8, 16 or 32 bit.

1. You can overlay up to 6 color channels on a conventional image. The color options available are Red, Green, Blue, Yellow, Cyan and Magenta.

1. I have added a option called Blending. Generally if a region has say both green and red channel, then the channel that is more strong is represented in the overlay. But you choose the Blending option, then the colors will be mixed and you will get a yellow color for regions having both Red and Green.

1. I have added a option called Histogram Stretching. When you run the Plugin, for each of the color channel, you will choose a threshold, intensities above which are only represented as the color pixel. If you choose threshold of 200 (the max is 255 for 8 bit color representation), then you have a range of 55 levels to represent the color data. By doing a histogram stretching, you stretch the histogram of the color pixel so that you have a range of close to 255 levels . So you have different shades of green or red and not just green or red. The histogram is stretched equally for all the colors, so that it is not skewed. Generally, you will not need to use this option for most cases.

1. When you run the plugin, it will check for all the open images and ask you for what color channel you want to use each image for. Incase you do not want to use any image/s for the overlay choose exclude from the drop down.

1. To install the plugin :- copy the plugin (Multi_Color_EM.jar file) in the Plugin folder of your ImageJ/Fiji installation and then restart ImageJ/Fiji. You should see in the Plugins drop down of ImageJ/Fiji which says "Multi Color EM", then you are set. In case you do not see it, try ImageJ-->Plugin --> Install (choose the Multi_Color_EM.jar) and restart ImageJ/Fiji.

1. Please refer “Multicolor Electron Microscopy for Simultaneous Visualization of Multiple Molecular Species.” Cell Chemical Biology,  2016 for additional details.
