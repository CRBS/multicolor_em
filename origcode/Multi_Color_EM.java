//This program was written by Ranjan Ramachandra and Paul Steinbach at
// National Center for Microscopy and Imaging Research (NCMIR),
//University of California, San Diego.
//For more details please see "Multicolor Electron Microscopy for Simultaneous Visualization of 
// Multiple Molecular Species" Adams et.al Cell Chemical Biology
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.WindowManager;
import java.lang.Math;
import ij.process.ImageStatistics; 

public class Multi_Color_EM implements PlugInFilter {
	ImagePlus imp;
	GenericDialog Gd;
	int[] windowList;
	String[] windowTitles;
	boolean Blending_Required;
	boolean Histogram_Stretching_Required;
	int[] Color_Channel_index;
	double[] Color_Channel_Threshold;
	int[] Image_Min;
	int[] Image_Max;
	int[] Image_depth;
	
	//To add new colors, you need to include them here first.
	String[] Channel_labels = {"Choose one", "Exclude", "Conventional", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta"};
	
	public int setup(String arg, ImagePlus imp) {
		
		this.imp = imp;
		return DOES_ALL;
		
	}

	public void run(ImageProcessor ip) {

		//Dynamic Global array declaration.
		windowList = WindowManager.getIDList();
		windowTitles = new String[windowList.length];
	        Color_Channel_index = new int[windowList.length];
	        Color_Channel_Threshold = new double[windowList.length];
	        Image_Min = new int[windowList.length];
	        Image_Max = new int[windowList.length];
	        Image_depth = new int[windowList.length];
		Gd = new GenericDialog("Multi Color EM");
		
		boolean Are_the_Input_Values_Correct;
		boolean Can_Create_Dialogbox = Check_if_images_are_fine();
		if (Can_Create_Dialogbox == true){
		        runDialog();
			Gd.showDialog();
			if (Gd.wasCanceled()){
				IJ.error("The Plugin was canceled!!!");
			}
			else {
				boolean Has_user_selected_correct_values = Check_the_User_Input();
				while (Has_user_selected_correct_values == false && Gd.wasCanceled() != true){
					Gd.wasCanceled();              //close the old dialog box and start a new one
					IJ.showMessage ("Please select correct values and try again");
					Gd = new GenericDialog("Multi Color EM");
					re_runDialog_with_previous_values();
					Gd.showDialog();
					Has_user_selected_correct_values = Check_the_User_Input();
				}

				if (Has_user_selected_correct_values == true && Gd.wasCanceled() != true){
				
					Create_the_Overlay_Image();
					
					//Once the overlay is created, throw the dialogbox to give the user chance to fine tune level of colors until he cancels the plug-in
					do {
						Gd.wasCanceled();			 //close the old dialog box and start a new one
						Gd = new GenericDialog("Multi Color EM");
						re_runDialog_with_previous_values();
						Gd.showDialog();
						Has_user_selected_correct_values = Check_the_User_Input();
						if (Has_user_selected_correct_values == true && Gd.wasCanceled() != true) Create_the_Overlay_Image();
					} while (Gd.wasCanceled() != true);
				
					IJ.showMessage ("Thanks for using Multi Color EM Plugin. Have a nice day");
				}		
			}		
		}
	}	

	//Purpose : Check for the images if they are of the same size etc.... 
	boolean Check_if_images_are_fine(){
		//int[] windowList = WindowManager.getIDList();
	 	ImagePlus TempImp;
	 	ImageProcessor TempIp;
		int[] Image_Height = new int[windowList.length];
		int[] Image_Width = new int[windowList.length];
		
		//Check if there are open images otherwise display error messages. This should anyway happen because this is a Pluginfilter
		if (windowList == null){
			IJ.noImage();
			return false;
		}
		if (windowList.length == 1){
			IJ.showMessage("You need atleast 2 images for this Plugin. One Conventional TEM image and one Elemental Map");
			return false;
		}
		
		for (int i = 0; i < windowList.length; i++){
			TempImp = WindowManager.getImage(windowList[i]);
			TempIp = TempImp.getProcessor();
			Image_Width[i] = TempIp.getWidth();
			Image_Height[i] = TempIp.getHeight();	
		}

		
		//Check if all the images are of the same size
		for (int i = 1;i < windowList.length; i++){
			if ( Image_Width[i] != Image_Width[(i-1)]){
				IJ.showMessage("All open images need to be of the same size.Please check and reload the correct images");
				return false;	
			}
			else if ( Image_Height[i] != Image_Height[(i-1)]){
				IJ.showMessage("All open images need to be of the same size.Please check and reload the correct images");
				return false;
			}
		}
	
		return true;
	}

	//Purpose : Creates the dialog box for the first time with default values
	void runDialog(){

		//Adding the fields to check the kind of chemical mapping used
		Gd.addMessage ("Developed by Ranjan Ramachandra and Paul Steinbach at NCMIR, UCSD.");
		boolean[] Check_box_default = {true, false};
		String[] Check_box_labels = { "Blending", "Histogram Stretching" };
		Gd.addCheckboxGroup(1,2,Check_box_labels,Check_box_default);
			
		//Now generating the dialox box depending on the number of images	
	 	for (int i = 0; i < windowList.length; i++){
			ImagePlus TempImp = WindowManager.getImage(windowList[i]);
			ImageProcessor TempIp = TempImp.getProcessor();
			//Test
			if (TempIp instanceof ByteProcessor)                    //This is a 8-bit image
				Image_depth[i] = 8;
			else if (TempIp instanceof ShortProcessor)   		//This is a 16-bit image
				Image_depth[i] = 16;
			else if (TempIp instanceof FloatProcessor)		//This is a 32-bit image
				Image_depth[i] = 32;	
			else if (TempIp instanceof ColorProcessor)		//This is a RGB image
				Image_depth[i] = 24;
				
			ImageStatistics stats = ImageStatistics.getStatistics(TempIp, ImageStatistics.MIN_MAX, null);
			Image_Min[i] = (int)(stats.min);
			Image_Max[i] = (int)(stats.max);
			int Image_Slide_Position = Image_Max[i]/2;
			windowTitles[i] = TempImp.getTitle();
			Gd.addChoice(windowTitles[i],Channel_labels,Channel_labels[0]);
			Gd.addSlider ("",Image_Min[i],Image_Max[i], Image_Slide_Position);
	 	}
	}

	//Purpose : Re-Creates the dialog box when the user has selected an erroneous value, it remebers the previous selections the user made.
	void re_runDialog_with_previous_values(){

		//Adding the fields to check the kind of chemical mapping used
		Gd.addMessage ("Developed by Ranjan Ramachandra and Paul Steinbach at NCMIR, UCSD.");
		boolean[] Check_box_default = {Blending_Required, Histogram_Stretching_Required};
		String[] Check_box_labels = { "Blending", "Histogram Stretching" };
		Gd.addCheckboxGroup(1,2,Check_box_labels,Check_box_default);
			
		//Now generating the dialox box depending on the number of images	
	 	for (int i = 0; i < windowList.length; i++){
			/*
			ImagePlus TempImp = WindowManager.getImage(windowList[i]);
			ImageProcessor TempIp = TempImp.getProcessor();
			//Test
			if (TempIp instanceof ByteProcessor)                    //This is a 8-bit image
				Image_depth[i] = 8;
			else if (TempIp instanceof ShortProcessor)   		//This is a 16-bit image
				Image_depth[i] = 16;
			else if (TempIp instanceof FloatProcessor)		//This is a 32-bit image
				Image_depth[i] = 32;	
			else if (TempIp instanceof ColorProcessor)		//This is a RGB image
				Image_depth[i] = 24;
				
			ImageStatistics stats = ImageStatistics.getStatistics(TempIp, ImageStatistics.MIN_MAX, null);
			Image_Min[i] = (int)(stats.min);
			Image_Max[i] = (int)(stats.max);
			int Image_Slide_Position = Image_Max[i]/2;
			windowTitles[i] = TempImp.getTitle();
			*/
			Gd.addChoice(windowTitles[i],Channel_labels,Channel_labels[Color_Channel_index[i]]);
			Gd.addSlider ("",Image_Min[i],Image_Max[i], Color_Channel_Threshold[i]);
	 	}
	}
	//Purpose : Checks for discrepancies in the input values the user has selected
	boolean Check_the_User_Input(){
		
		//Now getting the checkbox selection values
		Blending_Required = Gd.getNextBoolean();
		Histogram_Stretching_Required = Gd.getNextBoolean();
		
		//Now getting the color choice index (number corresponds to the drop down list)
		for (int i = 0; i < windowList.length; i++){
			Color_Channel_index[i] = Gd.getNextChoiceIndex();
		}
		
		//Now getting the Color threshold values
		for (int i = 0; i < windowList.length; i++){
			Color_Channel_Threshold[i] = Gd.getNextNumber();
		}
			
		//Checking to see if the drop down values have been selected for all the images
		for (int i = 0; i < windowList.length; i++){
			if (Color_Channel_index[i] == 0){
				IJ.showMessage ("The Color Channel has not been chosen for atleast one of the images");
				return false;
			}
		}

		//Checking to see if no two images have the same color option selected
		for (int i = 0; i < windowList.length; i++){
			if (Color_Channel_index[i] != 1){	//So that you can have as many excude options for mutliple images
				for (int k = (i + 1); k < windowList.length; k++){
					if (Color_Channel_index[k] != 1 && Color_Channel_index[k] == Color_Channel_index[i]){
						IJ.showMessage ("Any two images cannot have the same color channel");
						return false;	
					}
				}
			}
		}

		//Checking to see if atleast one of the images is a conventional image selection
		for (int i = 0; i < windowList.length; i++){
			if (Color_Channel_index[i] == 2){
				return true;
			}
		}
		IJ.showMessage ("There should be atleast one conventional Image");
		return false;		
	}

	//Purpose : This is the main part of the program which Creates the Final Overlay Image
	void Create_the_Overlay_Image(){

		ImagePlus DummyImp = WindowManager.getImage(windowList[0]);
		ImageProcessor DummyIp = DummyImp.getProcessor();
		
		//Creating the Overlay Image of RGB format
		String title = "Multi Color EM";
		int Image_Width = DummyIp.getWidth();
		int Image_Height = DummyIp.getHeight();
		ImagePlus OverlayImp = new ImagePlus();
		OverlayImp = NewImage.createRGBImage (title, Image_Width, Image_Height, 1, NewImage.FILL_BLACK);	
		ImageProcessor OverlayIp;
		OverlayIp = OverlayImp.getProcessor();
		
		//First get and copy the conventional image
		for (int i = 0; i < windowList.length; i++){
			if (Color_Channel_index[i] == 2){
				ImagePlus TEM_Grey_imp = WindowManager.getImage(windowList[i]);
				ImageProcessor TEM_Grey_Ip = TEM_Grey_imp.getProcessor();
				
				//Always make a copy of the original, so that original remains unchanged
				ImagePlus TEM_Grey_imp_Copy = new ImagePlus();
				TEM_Grey_imp_Copy = NewImage.createImage ("", Image_Width, Image_Height, 1, Image_depth[i],NewImage.FILL_BLACK);	
				ImageProcessor TEM_Grey_Ip_Copy = TEM_Grey_imp_Copy.getProcessor();
				TEM_Grey_Ip_Copy.copyBits(TEM_Grey_Ip, 0, 0, Blitter.COPY);
				
				//Now convert to 8 bits and copy to the overlay image.
				ImageConverter iConv = new ImageConverter(TEM_Grey_imp_Copy);
				//iConv.setDoScaling(true);
				iConv.convertToGray8();
				TEM_Grey_Ip_Copy = TEM_Grey_imp_Copy.getProcessor();
				OverlayIp.copyBits(TEM_Grey_Ip, 0, 0, Blitter.COPY);	
			}
		}    // End of copying the conventional Image

		
		//Now convert image to a 2-D array for faster processing
		int Overlay_Pixels [][] =  OverlayIp.getIntArray();

		//Now color code the pixels that were made zero in the overlay image with the respective colors....
		for (int i = 0; i < windowList.length; i++){
			if (Color_Channel_index[i] != 1 && Color_Channel_index[i] != 2){              //Not for the conventional Image or the Images that have exclude option
				
				ImagePlus TempImp = WindowManager.getImage(windowList[i]);
				ImageProcessor TempIp = TempImp.getProcessor();
				
				//Always make a copy of the original, so that original remains unchanged
				ImagePlus TempImp_Copy = new ImagePlus();
				TempImp_Copy = NewImage.createImage ("", Image_Width, Image_Height, 1, Image_depth[i],NewImage.FILL_BLACK);	
				ImageProcessor TempIp_Copy = TempImp_Copy.getProcessor();
				TempIp_Copy.copyBits(TempIp, 0, 0, Blitter.COPY);

				//Check if histogram stretching is required	
				if (Histogram_Stretching_Required == true)	
					TempIp_Copy = Stretch_the_Histogram (TempIp_Copy, Image_Height, Image_Width, Image_depth[i]);
				
				//Now convert it into 8-bits
				ImageConverter iConv = new ImageConverter(TempImp_Copy);
				//iConv.setDoScaling(true);
				iConv.convertToGray8();
				TempIp_Copy = TempImp_Copy.getProcessor();
				int Temp_Pixels [][] = TempIp_Copy.getIntArray();
				
				//Now get the threshold value after conversion to 8-bit
				int Color_Channel_Threshold_after_conversion = Get_the_converted_threshold (Image_Min[i], Image_Max[i], Image_depth[i], Color_Channel_Threshold[i]);				
				
				for (int u = 0; u < Image_Width; u++){
					for (int v = 0; v < Image_Height; v++){
						if ( Temp_Pixels [u][v] >= Color_Channel_Threshold_after_conversion){
							Overlay_Pixels [u][v] = Get_the_Colorized_Pixel(Color_Channel_index[i], Temp_Pixels [u][v] , Overlay_Pixels [u][v]);
						}
					}
				}
			}
		}	

		
		if (Blending_Required == true){
			Overlay_Pixels = Restretch_the_Pixels_for_Blending_Option (Overlay_Pixels, Image_Height, Image_Width);
		}

		
		//First copy the GreyScale TEM image into an array. Then use this to add the greyscale TEM  with transparency to the color values 
		for (int i = 0; i < windowList.length; i++){
			if (Color_Channel_index[i] == 2){
				ImagePlus TEM_Grey_imp = WindowManager.getImage(windowList[i]);
				ImageProcessor TEM_Grey_Ip = TEM_Grey_imp.getProcessor();
				
				//Always make a copy of the original, so that original remains unchanged
				ImagePlus TEM_Grey_imp_Copy = new ImagePlus();
				TEM_Grey_imp_Copy = NewImage.createImage ("", Image_Width, Image_Height, 1, Image_depth[i],NewImage.FILL_BLACK);	
				ImageProcessor TEM_Grey_Ip_Copy = TEM_Grey_imp_Copy.getProcessor();
				TEM_Grey_Ip_Copy.copyBits(TEM_Grey_Ip, 0, 0, Blitter.COPY);
				
				ImageConverter iConv = new ImageConverter(TEM_Grey_imp_Copy);
				//iConv.setDoScaling(true);
				iConv.convertToGray8();
				TEM_Grey_Ip_Copy = TEM_Grey_imp_Copy.getProcessor();
				int TEM_Grey_PIxels [][] = TEM_Grey_Ip_Copy.getIntArray();
				
				for (int u = 0; u < Image_Width; u++){
					for (int v = 0; v < Image_Height; v++){
						Overlay_Pixels [u][v] = Add_the_Grey_value_with_transparency_on_Color (TEM_Grey_PIxels [u][v], Overlay_Pixels [u][v]);
					}
				}
			}
		}    // End of adding the greyscale with transparency to the color values
		
		OverlayIp.setIntArray(Overlay_Pixels);
		OverlayImp.show();
	}

	//Purpose : Converts the individual greyscale elemental maps or images images to their respective colors.
	int Get_the_Colorized_Pixel(int Color_Required, int Grey_Pixel, int Color_Pixel){

		int Red_Value = (Color_Pixel & 0xff0000) >> 16;
		int Green_Value = (Color_Pixel & 0x00ff00) >> 8;
		int Blue_Value = (Color_Pixel & 0x0000ff);
		 
		switch (Color_Required){
			case 0: IJ.showMessage("There is some problem with the program logic. You can not get colorized pixel, when no color is selected");
				break;
			case 1: IJ.showMessage("There is some problem with the program logic. You can not get colorized pixel, when you have excluded the image");
				break;
			case 2: IJ.showMessage("There is some problem with the program logic. You can not get colorized pixel, when conventional is selected");
				break;
			//Now for the red color
			case 3: if (Blending_Required == false){
					if (Red_Value == Green_Value && Red_Value == Blue_Value){                         //Which means it is a grey pixel and no color subsitution has been done yet.
						Red_Value = Grey_Pixel;
						Green_Value = 0;
						Blue_Value = 0;	
					}
					else if	(Grey_Pixel > Red_Value && Grey_Pixel > Green_Value && Grey_Pixel > Blue_Value){           // Do the red color subsitution only if intensity is greater than intensity of the color already present
						Red_Value = Grey_Pixel;
						Green_Value = 0;
						Blue_Value = 0;
					}
				}
				else {					//If Blending is required
					if (Red_Value == Green_Value && Red_Value == Blue_Value ){            //when first time color entry into the pixel divide by 2 to accomdate the blending of mixed colors
						Red_Value = Grey_Pixel / 3;
						Green_Value = 0;
						Blue_Value = 0;	
						Color_Pixel = ((Red_Value & 0xff)<<16)|((Green_Value & 0xff)<<8)|Blue_Value & 0xff;
					}
					else{				
						Red_Value = Red_Value + (Grey_Pixel / 3); if (Red_Value > 255) Red_Value = 255;
						Color_Pixel = ((Red_Value & 0xff)<<16)|((Green_Value & 0xff)<<8)|Blue_Value & 0xff;
					}
				}// End of Blending option
				break;
			//Now for the Green color
			case 4: if (Blending_Required == false){
					if (Red_Value == Green_Value && Red_Value == Blue_Value){			//Which means it is a grey pixel and no color subsitution has been done yet.
						Red_Value = 0;
						Green_Value = Grey_Pixel;
						Blue_Value = 0;
					}
					else if  (Grey_Pixel > Red_Value && Grey_Pixel > Green_Value && Grey_Pixel > Blue_Value){           // Do the green color subsitution only if intensity is greater than intensity of the color already present
						Red_Value = 0;
						Green_Value = Grey_Pixel;
						Blue_Value = 0;
					}					
				}
				else {					//If Blending is required
					if (Red_Value == Green_Value && Red_Value == Blue_Value){                   //when first time color entry into the pixel divide by 2 to accomdate the blending of mixed colors
						Red_Value = 0;
						Green_Value = Grey_Pixel / 3;
						Blue_Value = 0;
					}
					else{				
						Green_Value = Green_Value + (Grey_Pixel / 3); if ( Green_Value > 255 ) Green_Value = 255;
					}
				}// End of Blending option
				break;
			//Now for the blue color
			case 5: if (Blending_Required == false){
					if (Red_Value == Green_Value && Red_Value == Blue_Value){			//Which means it is a grey pixel and no color subsitution has been done yet.
						Red_Value = 0;
						Green_Value = 0;
						Blue_Value = Grey_Pixel;
					}
					else if  (Grey_Pixel > Red_Value && Grey_Pixel > Green_Value && Grey_Pixel > Blue_Value){           // Do the blue color subsitution only if intensity is greater than intensity of the color already present
						Red_Value = 0;
						Green_Value = 0;
						Blue_Value = Grey_Pixel;
					}		
				}
				else {					//If Blending is required
					if (Red_Value == Green_Value && Red_Value == Blue_Value ){            //when first time color entry into the pixel divide by 2 to accomdate the blending of mixed colors
						Red_Value = 0;
						Green_Value = 0;
						Blue_Value = Grey_Pixel / 3;
					}
					else{				
						Blue_Value = Blue_Value + (Grey_Pixel / 3); if ( Blue_Value > 255) Blue_Value = 255;
					}
				}// End of Blending option
				break;
			//Now for the Yellow Color
			case 6: if (Blending_Required == false){
					if (Red_Value == Green_Value && Red_Value == Blue_Value){			//Which means it is a grey pixel and no color subsitution has been done yet.
						Red_Value = Grey_Pixel;
						Green_Value = Grey_Pixel;
						Blue_Value = 0;
					}
					else if  (Grey_Pixel > Red_Value && Grey_Pixel > Green_Value && Grey_Pixel > Blue_Value){           // Do the yellow color subsitution only if intensity is greater than intensity of the color already present
						Red_Value = Grey_Pixel;
						Green_Value = Grey_Pixel;
						Blue_Value = 0;
					}
				}
				else {					//If Blending is required
					if (Red_Value == Green_Value && Red_Value == Blue_Value){            //when first time color entry into the pixel divide by 2 to accomdate the blending of mixed colors
						Red_Value = Grey_Pixel / 3;
						Green_Value = Grey_Pixel / 3;
						Blue_Value = 0;
					}
					else{				
						Red_Value = Red_Value + (Grey_Pixel / 3); if ( Red_Value > 255) Red_Value = 255;
						Green_Value = Green_Value + (Grey_Pixel / 3); if ( Green_Value > 255) Green_Value = 255;
					}
				}// End of Blending option
				break;
			//Now for the Cyan Color
			case 7: if (Blending_Required == false){
					if (Red_Value == Green_Value && Red_Value == Blue_Value){			//Which means it is a grey pixel and no color subsitution has been done yet.
						Red_Value = 0;
						Green_Value = Grey_Pixel;
						Blue_Value = Grey_Pixel;
					}
					else if  (Grey_Pixel > Red_Value && Grey_Pixel > Green_Value && Grey_Pixel > Blue_Value){           // Do the cyan color subsitution only if intensity is greater than intensity of the color already present
						Red_Value = 0;
						Green_Value = Grey_Pixel;
						Blue_Value = Grey_Pixel;				
					}
				}
				else {					//If Blending is required
					if (Red_Value == Green_Value && Red_Value == Blue_Value){            //when first time color entry into the pixel divide by 2 to accomdate the blending of mixed colors
						Red_Value = 0;
						Green_Value = Grey_Pixel / 3;
						Blue_Value = Grey_Pixel / 3;
					}
					else{				
						Green_Value = Green_Value + (Grey_Pixel / 3); if ( Green_Value > 255) Green_Value = 255;
						Blue_Value = Blue_Value + (Grey_Pixel / 3); if ( Blue_Value > 255) Blue_Value = 255;
					}
				}// End of Blending option
				break;
			//Now for Magenta
			case 8: if (Blending_Required == false){
					if (Red_Value == Green_Value && Red_Value == Blue_Value){			//Which means it is a grey pixel and no color subsitution has been done yet.
						Red_Value = Grey_Pixel;
						Green_Value = 0;
						Blue_Value = Grey_Pixel;
					}
					else if  (Grey_Pixel > Red_Value && Grey_Pixel > Green_Value && Grey_Pixel > Blue_Value){           // Do the Magenta color subsitution only if intensity is greater than intensity of the color already present
						Red_Value = Grey_Pixel;
						Green_Value = 0;
						Blue_Value = Grey_Pixel;
					}
				}
				else {					//If Blending is required
					if (Red_Value == Green_Value && Red_Value == Blue_Value){            //when first time color entry into the pixel divide by 2 to accomdate the blending of mixed colors
						Red_Value = Grey_Pixel / 3;
						Green_Value = 0;
						Blue_Value = Grey_Pixel / 3;
					}
					else{				//Mixed Color combination (Like Magenta + Red)
						Red_Value = Red_Value + (Grey_Pixel / 3); if ( Red_Value > 255 ) Red_Value = 255;
						Blue_Value = Blue_Value + (Grey_Pixel / 3); if ( Blue_Value > 255) Blue_Value = 255;
					}
				}// End of Blending option
				break;
		}

		if (Red_Value > 255) Red_Value = 255;
		if (Green_Value > 255) Green_Value = 255;
		if (Blue_Value > 255) Blue_Value = 255;
		
		Color_Pixel = ((Red_Value & 0xff)<<16)|((Green_Value & 0xff)<<8)|Blue_Value & 0xff;
		return Color_Pixel;
	}



	//Purpose : When blending is used, the pixel values are divided by 3, to accomdate for the mixed colors. But for example ,if the color combination is pure (Red + Green + Blue) then you can Re-stretch the pixels.
	//          The color values can not be read from all the images simulatanously because the images can be very big. This is a kind of workaround.
	int [][] Restretch_the_Pixels_for_Blending_Option (int [][] The_Pixels, int Image_Height, int Image_Width){
		// First check if all the color pixels are less than half full
		boolean All_Color_Pixels_Less_Than_half_full = true;
		boolean All_Color_Pixels_Less_Than_third_full = true;
		 
		for (int u = 0; u < Image_Width; u++){
			for (int v = 0; v < Image_Height; v++){
				int Color_Pixel = The_Pixels [u][v]; 	
				int Red_Value = (Color_Pixel & 0xff0000) >> 16;
				int Green_Value = (Color_Pixel & 0x00ff00) >> 8;
				int Blue_Value = (Color_Pixel & 0x0000ff);
				if ( Red_Value != Green_Value || Red_Value != Blue_Value){   //This means this is a color pixel and not a conventional pixel.
					if ( Red_Value > 255/2 || Green_Value > 255/2 || Blue_Value > 255/2){     
						All_Color_Pixels_Less_Than_half_full = false;
						All_Color_Pixels_Less_Than_third_full = false;
						return The_Pixels;
					}
					else if ( Red_Value > 255/3 || Green_Value > 255/3 || Blue_Value > 255/3){
						All_Color_Pixels_Less_Than_third_full = false;	
					}
				}
			}
		}
		// Now above check is successfull then execute the code below.
		
		if ( All_Color_Pixels_Less_Than_third_full == true){
			for (int u = 0; u < Image_Width; u++){
				for (int v = 0; v < Image_Height; v++){
					int Color_Pixel = The_Pixels [u][v]; 	
					int Red_Value = (Color_Pixel & 0xff0000) >> 16;
					int Green_Value = (Color_Pixel & 0x00ff00) >> 8;
					int Blue_Value = (Color_Pixel & 0x0000ff);
					if ( Red_Value != Green_Value || Red_Value != Blue_Value){   //This means this is a color pixel and not a conventional pixel.
						if ( Red_Value <= 255/3 && Green_Value <= 255/3 && Blue_Value <= 255/3){
							Red_Value = Red_Value * 3; if (Red_Value > 255) Red_Value = 255; 
							Green_Value = Green_Value * 3; if (Green_Value > 255) Green_Value = 255;
							Blue_Value = Blue_Value * 3; if (Blue_Value > 255) Blue_Value = 255;
							Color_Pixel = ((Red_Value & 0xff)<<16)|((Green_Value & 0xff)<<8)|Blue_Value & 0xff;
							The_Pixels [u][v] = Color_Pixel;
						}
					}
				}
			}
		}
		
		else if ( All_Color_Pixels_Less_Than_half_full == true){
			for (int u = 0; u < Image_Width; u++){
				for (int v = 0; v < Image_Height; v++){
					int Color_Pixel = The_Pixels [u][v]; 	
					int Red_Value = (Color_Pixel & 0xff0000) >> 16;
					int Green_Value = (Color_Pixel & 0x00ff00) >> 8;
					int Blue_Value = (Color_Pixel & 0x0000ff);
					if ( Red_Value != Green_Value || Red_Value != Blue_Value){   //This means this is a color pixel and not a conventional pixel.
						if ( Red_Value <= 255/2 && Green_Value <= 255/2 && Blue_Value <= 255/2){
							Red_Value = Red_Value * 2; if (Red_Value > 255) Red_Value = 255; 
							Green_Value = Green_Value * 2; if (Green_Value > 255) Green_Value = 255;
							Blue_Value = Blue_Value * 2; if (Blue_Value > 255) Blue_Value = 255;
							Color_Pixel = ((Red_Value & 0xff)<<16)|((Green_Value & 0xff)<<8)|Blue_Value & 0xff;
							The_Pixels [u][v] = Color_Pixel;
						}
					}
				}
			}
		}
		return The_Pixels;
	}

	//Purpose :  This calculates the transparency of the color or how much grey of the TEM conventional image should the color pixel have.
	int Add_the_Grey_value_with_transparency_on_Color (int TEM_Grey_Pixel, int Overlay_Color_Pixel){
		
		double Transparency_Value;
		int Overlaid_Grey_on_color_value;

		int Red_Value = (Overlay_Color_Pixel & 0xff0000) >> 16; 
		int Green_Value = (Overlay_Color_Pixel & 0x00ff00) >> 8; 
		int Blue_Value = (Overlay_Color_Pixel & 0x0000ff); 

		if ( Red_Value != Green_Value || Red_Value != Blue_Value) {  			   //This means this is a color pixel and not a conventional pixel.
		
			int Maximum_color_Value = Math.max(Math.max(Red_Value,Green_Value),Blue_Value);
			Transparency_Value = Maximum_color_Value / 255.0 ; if ( Transparency_Value > 1 ) Transparency_Value = 1; if ( Transparency_Value < 0 ) Transparency_Value = 0;

			Red_Value = Red_Value + (int) ((1 - Transparency_Value) * TEM_Grey_Pixel); if (Red_Value > 255) Red_Value = 255;
			Green_Value = Green_Value + (int) ((1 - Transparency_Value) * TEM_Grey_Pixel); if (Green_Value > 255) Green_Value = 255;
			Blue_Value = Blue_Value + (int) ((1 - Transparency_Value) * TEM_Grey_Pixel); if (Blue_Value > 255) Blue_Value = 255;

		}
			
		Overlaid_Grey_on_color_value = ((Red_Value & 0xff)<<16)|((Green_Value & 0xff)<<8)|Blue_Value & 0xff;
		return Overlaid_Grey_on_color_value;
									
	}

	//Purpose : When a 16 bit or 32 bit image is converted to 8-bit internally by imageJ, it is difficult to know what would be corresponding value of the user selected threshold in 8-bits.
	//So creates a dummy image of 3 pixels, having the max, min and threshold values of the image. Then, converts it into 8-bit and finds out what the threshold will be now.
	int Get_the_converted_threshold (int Image_Minimum,int Image_Maximum,int Image_depth_bits, double Color_Channel_Threshold){

		int The_converted_Threshold;
		ImagePlus Threshold_ImageImp = new ImagePlus();
		Threshold_ImageImp = NewImage.createImage ("Threshold_Image", 3, 1, 1, Image_depth_bits,NewImage.FILL_BLACK);	
		ImageProcessor Threshold_ImageIp = Threshold_ImageImp.getProcessor();
		Threshold_ImageIp.putPixel (0,0,Image_Minimum);
		Threshold_ImageIp.putPixel (1,0,Image_Maximum);
		Threshold_ImageIp.putPixel (2,0,(int)(Color_Channel_Threshold));

		//Check if histogram stretching is required	
		if (Histogram_Stretching_Required == true)	
			Threshold_ImageIp = Stretch_the_Histogram (Threshold_ImageIp, 1, 3, Image_depth_bits);
		
		ImageConverter iConv = new ImageConverter(Threshold_ImageImp);
		//iConv.setDoScaling(true);
		iConv.convertToGray8();
		Threshold_ImageIp = Threshold_ImageImp.getProcessor();
		The_converted_Threshold = Threshold_ImageIp.getPixel(2,0);

		return The_converted_Threshold;
		
	}
	
	//Purpose : To stretch the Histogram. Suppose the threshold value for a 8-bit is selected to be 200, then there are only 55 levels for the representation of the color. 
	//What this does is for levels (200 to 255) is strecthed to occupy (40 to 255), so you will have more degrees of color.
	
	ImageProcessor Stretch_the_Histogram (ImageProcessor Elemental_Ip, int Image_Height, int Image_Width, int Image_depth_bits){
		// To keep the stretching equal for all the colors, the Fmin and Fmax are chosen as the least and max values of all the grey elemental images
		double Fmin = 65535;      //use the threshold rather than Image_min because you want stretch only those pixels whose intensity is above the threshold.
		double Fmax = 0;
		
		int The_Pixels[][] = Elemental_Ip.getIntArray();
		
		for (int i = 0; i < windowList.length; i++){
			if ( Color_Channel_index[i] != 2 && Color_Channel_index[i] != 1 && Color_Channel_Threshold[i] < Fmin) Fmin = Color_Channel_Threshold[i];        //exclude the Conventional TEM image
		}

		for (int i = 0; i < windowList.length; i++){
			if ( Color_Channel_index[i] != 2 && Color_Channel_index[i] != 1 && Image_Max[i] > Fmax) Fmax = Image_Max[i];        //exclude the Conventional TEM image
		}


		
		//The Histogram stretching alogorithm stretches from 0 to maximum value. You dont want to start from zero but 10000 for 16 or 32 bit and 40 for 8 bit images
		if (Image_depth_bits == 16 || Image_depth_bits == 32){                              //for a 16 or 32 bit image 
		
			Fmin = (((10000.0 * Fmax / 65535.0) - Fmin) / (10000.0/65535.0 - 1));         //modify Fmin so that the threshold intensity doesnt start from 0 but 10000
			for (int u = 0; u < Image_Width; u++){
				for (int v = 0; v < Image_Height; v++){
					The_Pixels[u][v] = (int)((The_Pixels[u][v] - Fmin) * 65535 /(Fmax - Fmin));
					if (The_Pixels[u][v] < 0) The_Pixels[u][v] = 0;
					if (The_Pixels[u][v] > 65535) The_Pixels[u][v] = 65535; 
				}
			}
		}
		else						//for 8 bit or RGB image
		{
			Fmin = (((40.0 * Fmax / 255.0) - Fmin) / (40.0/255.0 - 1));         //modify Fmin so that the threshold intensity doesnt start from 0 but 40
			for (int u = 0; u < Image_Width; u++){
				for (int v = 0; v < Image_Height; v++){
					The_Pixels[u][v] = (int)((The_Pixels[u][v] - Fmin) * 255 /(Fmax - Fmin));
					if (The_Pixels[u][v] <0) The_Pixels[u][v] = 0;
					if (The_Pixels[u][v] > 255) The_Pixels[u][v] = 255;
				}
			}
		}

		
		Elemental_Ip.setIntArray(The_Pixels);
		return Elemental_Ip;
	}

}
	
