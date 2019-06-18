/**
 * Simple test of the Predator controller.
 *
 * Based on Simple.java from LabJack.
 */
import java.io.*;
import java.text.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.labjack.LJUD;
import com.labjack.LJUDException;

public class Predator {

	public Predator() {
	}

	private void handleLJUDException(LJUDException e) {
		e.printStackTrace();
		if(e.getError() > LJUD.Errors.MIN_GROUP_ERROR.getValue()) {
			System.exit(-1);
		}
	}

	//Displays warning message if there is one. Error values < 0 are warnings
	//and do not cause a LJUDException in the LJUD class.
	private void checkForWarning(int error) {
		Pointer errorStringPtr = new Memory(256);
		if(error < 0) {
			LJUD.errorToString(error, errorStringPtr);
			System.out.println("Warning: " + errorStringPtr.getString(0).trim());
		}
	}

	public void runExample() {
		try {
			int intErrorcode;
			IntByReference refIOType = new IntByReference(0);
			IntByReference refChannel = new IntByReference(0);
			DoubleByReference refValue = new DoubleByReference(0);
			int intIOType = 0;
			int intChannel = 0;
			double dblValue = 0.0;
			double value0 = 9999, value1 = 9999, value2 = 9999;
			double valueDIBit = 9999, valueDIPort = 9999, valueCounter = 9999;
			int intHandle = 0;
			IntByReference refHandle = new IntByReference(0);
			IntByReference dummyInt = new IntByReference(0);
			DoubleByReference dummyDouble = new DoubleByReference(0.0);
			boolean isDone = false;

			NumberFormat formatter = new DecimalFormat("0.000");
			String line = "";
			BufferedReader br = new BufferedReader(
					new InputStreamReader(System.in));
	
			//Read and display the UD versions.
			System.out.println("UD Driver Version = " + formatter.format(LJUD.getDriverVersion()));
	
			//Open the first found LabJack U3.
			intErrorcode = LJUD.openLabJack(LJUD.Constants.dtU3, LJUD.Constants.ctUSB, "1", 1, refHandle);
			checkForWarning(intErrorcode);
			intHandle = refHandle.getValue();

			int i = 0;
			double voltage = 0;
			int maxPort = 15;
			for (i = 0; i <= maxPort; i++) {
			    voltage = i/(maxPort + 0.0) * 5;
			    System.out.println("Port is " + i + "Voltage is " + voltage);
			    // The general form of the AddRequest function is:
			    // AddRequest (Handle, IOType, Channel, Value, x1, UserData)
			    // https://labjack.com/support/datasheets/u3/high-level-driver/overview
			    LJUD.addRequest(intHandle, LJUD.Constants.ioPUT_DAC, 0, voltage, 0, 0);
			    LJUD.addRequest(intHandle, LJUD.Constants.ioPUT_DAC, 1, voltage, 0, 0);
			    
			    //Set digital output to output-high.
			    LJUD.addRequest(intHandle, LJUD.Constants.ioPUT_DIGITAL_BIT, i, 1, 0, 0);

			    if (i>0) {
				// See 2.8 - Digital I/O of LabJack U3 Datasheet (local PDF):

				// "In some cases, an open-collector style output can be used to get a 5V
				// signal. To get a low set the line to output-low, and to get a high set
				// the line to input. When the line is set to input, the voltage on the
				// line is determined by a pull-up resistor. The U3 has an internal ~100k
				// resistor to 3.3V, but an external resistor can be added to a different
				// voltage. Whether this will work depends on how much current the load
				// is going to draw and what the required logic thresholds are. Say for
				// example a 10k resistor is added from EIO0 to VS. EIO0 has an internal
				// 100k pull-up to 3.3 volts and a series output resistance of about 180
				// ohms. Assume the load draws just a few microamps or less and thus is
				// negligible. When EIO0 is set to input, there will be 100k to 3.3 volts
				// in parallel with 10k to 5 volts, and thus the line will sit at about
				// 4.85 volts. When the line is set to output-low, there will be 180 ohms
				// in series with the 10k, so the line will be pulled down to about 0.1
				// volts"

				// What this means is that

				//     to turn the relay off, set the
				//     LabJack Port (FIO0-FIO7,
				//     EIO0-EIO7) to be an input (DI).

				//     to turn the relay on, set the
				//     port to be an output (DO) with
				//     the Voltage/State checked.
				LJUD.addRequest(intHandle, LJUD.Constants.ioGET_DIGITAL_BIT, i-1, 0, 0, 0);
			    }

			    //Execute the requests.
			    intErrorcode = LJUD.goOne(intHandle);
			    checkForWarning(intErrorcode);
			    System.out.println("goOne returned " + intErrorcode);

			    //Get all the results. The input measurement results are stored.
			    //All other results are for configuration or output requests so 
			    //we are just checking whether there was an error.
			    LJUD.getFirstResult(intHandle, refIOType, refChannel, refValue, dummyInt, dummyDouble);
	
			    isDone = false;
			    try {
				while(!isDone) {
				    intIOType = refIOType.getValue();
				    intChannel = refChannel.getValue();
				    dblValue = refValue.getValue();
				    System.out.println("Results: ioType (See doc/constant-values.html): " + intIOType
						       + " channel: " + intChannel
						       + " value: " + dblValue);
				    // if(intIOType == LJUD.Constants.ioGET_AIN) {
				    // 	if(intChannel == 0)
				    // 		value0 = dblValue;
				    // 	if(intChannel == 1)
				    // 		value1 = dblValue;
				    // }
		
				    // if(intIOType == LJUD.Constants.ioGET_AIN_DIFF)
				    // 	value2 = dblValue;
		
				    // if(intIOType == LJUD.Constants.ioGET_DIGITAL_BIT)
				    // 	valueDIBit = dblValue;
		
				    // if(intIOType == LJUD.Constants.ioGET_DIGITAL_PORT)
				    // 	valueDIPort = dblValue;
		
				    // if(intIOType == LJUD.Constants.ioGET_COUNTER)
				    // 	valueCounter = dblValue;
		
				    LJUD.getNextResult(intHandle, refIOType, refChannel, refValue, dummyInt, dummyDouble);
				}
			    }
			    catch(LJUDException le) {
				if(le.getError() == LJUD.Errors.NO_MORE_DATA_AVAILABLE.getValue()) {
				    isDone = true;
				}
				else {
				    throw le;
				}
			    }

			    //Get the result of the DAC0 request just to check for an errorcode.
			    //The general form of the GetResult function is:
			    //GetResult (Handle, IOType, Channel, *Value)
			    // lngErrorcode = GetResult (lngHandle, LJ_ioPUT_DAC, 0, 0);
			    Thread.sleep(2000);
                        } 
			// Set the last relay to off
			LJUD.addRequest(intHandle, LJUD.Constants.ioGET_DIGITAL_BIT, maxPort, 0, 0, 0);
			intErrorcode = LJUD.goOne(intHandle);
			checkForWarning(intErrorcode);
			System.out.println("Turning the last relay off: goOne returned " + intErrorcode);

			// //Start by using the pin_configuration_reset IOType so that all
			// //pin assignments are in the factory default condition.
			// LJUD.ePut(intHandle, LJUD.Constants.ioPIN_CONFIGURATION_RESET, 0, 0, 0);
	
			// //First some configuration commands. These will be done with the ePut
			// //function which combines the add/go/get into a single call.
	
			// //Configure FIO0 and FIO3 as analog, all else as digital. That means we
			// //will start from channel 0 and update all 16 flexible bits. We will
			// //pass a value of b0000000000001111 or d15.
			// LJUD.ePut(intHandle, LJUD.Constants.ioPUT_ANALOG_ENABLE_PORT, 0, 15, 16);
	
			// //Set the timer/counter pin offset to 7, which will put the first
			// //timer/counter on FIO7.
			// LJUD.ePut(intHandle, LJUD.Constants.ioPUT_CONFIG,
			// 		LJUD.Constants.chTIMER_COUNTER_PIN_OFFSET, 7, 0);
			
			// //Enable Counter1 (FIO7).
			// LJUD.ePut(intHandle, LJUD.Constants.ioPUT_COUNTER_ENABLE, 1, 1, 0);
			
			// //The following commands will use the add-go-get method to group
			// //multiple requests into a single low-level function.
	
			// //Request a single-ended reading from AIN0.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioGET_AIN, 0, 0, 0, 0);
			
			// //Request a single-ended reading from AIN1.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioGET_AIN, 1, 0, 0, 0);
			
			// //Request a reading from AIN2 using the Special range.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioGET_AIN_DIFF, 2, 0, 32, 0);
			
			// //Set DAC0 to 3.5 volts.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioPUT_DAC, 0, 3.5, 0, 0);
			
			// //Set digital output FIO4 to output-high.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioPUT_DIGITAL_BIT, 4, 1,	0, 0);
			
			// //Read digital input FIO5.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioGET_DIGITAL_BIT, 5, 0, 0, 0);
			
			// //Read digital inputs FIO5 through FIO6.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioGET_DIGITAL_PORT, 5, 0, 2, 0);
			
			// //Request the value of Counter1.
			// LJUD.addRequest(intHandle, LJUD.Constants.ioGET_COUNTER, 1, 0, 0, 0);
			
			// while(true) {
			// 	//Execute the requests.
			// 	LJUD.goOne(intHandle);
	
			// 	//Get all the results. The input measurement results are stored.
			// 	//All other results are for configuration or output requests so 
			// 	//we are just checking whether there was an error.
			// 	LJUD.getFirstResult(intHandle, refIOType, refChannel, refValue, dummyInt, dummyDouble);
	
			// 	isDone = false;
			// 	try {
			// 		while(!isDone) {
			// 			intIOType = refIOType.getValue();
			// 			intChannel = refChannel.getValue();
			// 			dblValue = refValue.getValue();
			// 			if(intIOType == LJUD.Constants.ioGET_AIN) {
			// 				if(intChannel == 0)
			// 					value0 = dblValue;
			// 				if(intChannel == 1)
			// 					value1 = dblValue;
			// 			}
		
			// 			if(intIOType == LJUD.Constants.ioGET_AIN_DIFF)
			// 				value2 = dblValue;
		
			// 			if(intIOType == LJUD.Constants.ioGET_DIGITAL_BIT)
			// 				valueDIBit = dblValue;
		
			// 			if(intIOType == LJUD.Constants.ioGET_DIGITAL_PORT)
			// 				valueDIPort = dblValue;
		
			// 			if(intIOType == LJUD.Constants.ioGET_COUNTER)
			// 				valueCounter = dblValue;
		
			// 			LJUD.getNextResult(intHandle, refIOType, refChannel, refValue, dummyInt, dummyDouble);
			// 		}
			// 	}
			// 	catch(LJUDException le) {
			// 		if(le.getError() == LJUD.Errors.NO_MORE_DATA_AVAILABLE.getValue()) {
			// 			isDone = true;
			// 		}
			// 		else {
			// 			throw le;
			// 		}
			// 	}
	
			// 	System.out.println("AIN0 = " + value0);
			// 	System.out.println("AIN1 = " + value1);
			// 	System.out.println("AIN2 = " + value2);
			// 	System.out.println("FIO5 = " + valueDIBit);
			// 	System.out.println("FIO5-FIO6 = " + valueDIPort);	//Will read 3 (binary 11) if both lines are pulled-high as normal.
			// 	System.out.println("Counter1 (FIO7) " + valueCounter);
	
			// 	System.out.println("\nPress Enter to go again or (q) and Enter to quit");
			// 	line = br.readLine().toUpperCase().trim();
			// 	if(line.equals("Q")) {
			// 		return;
			// 	}
			// }
		}
		catch(LJUDException le) {
			handleLJUDException(le);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Predator().runExample();
	}

}
