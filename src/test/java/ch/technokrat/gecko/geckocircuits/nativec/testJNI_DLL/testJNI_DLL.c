/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  @author DIEHL Controls Ricardo Richter
 */

#include <jni.h>
#include "testJNI_DLL.h"

/**
 * Do NOT edit this function
 */
JNIEXPORT void JNICALL Java_gecko_geckocircuits_nativec_NativeCWrapper_calcOutputs
  (JNIEnv *env, jobject jObj, jdoubleArray xIn, jdoubleArray xOut, jint outLength, jdouble jtime, jdouble jdeltaT) {

   jboolean isCopyIn, isCopyOut;
   // get array length of xIn
   jint inputLength = (*env)->GetArrayLength(env, xIn);
   double *inputArray = (double *) (*env)->GetDoubleArrayElements(env, xIn, &isCopyIn);
   // get pointer to primitive output array
   double *outputArray = (*env)->GetDoubleArrayElements(env, xOut, &isCopyOut);

   /** call the function that needs to be implemented */
   calcOutputs(inputArray, (int) inputLength, outputArray, (int) outLength, (double) jtime, (double) jdeltaT);
   if (isCopyIn == JNI_TRUE) {
      // copy elements back to the java array and free buffer of inputArray
      (*env)->ReleaseDoubleArrayElements(env, xIn, inputArray, 0);
   }
   if (isCopyOut == JNI_TRUE) {
      // copy elements back to the java array and free buffer of outputArray
      (*env)->ReleaseDoubleArrayElements(env, xOut, outputArray, 0);
   }
}

/**
 *  Do NOT edit this function
 */
JNIEXPORT void JNICALL Java_gecko_geckocircuits_nativec_NativeCWrapper_initParameters
  (JNIEnv *env, jobject jObj) {
	initParameters ();
}

/**
 *  This function needs to be implemented according to your needs. It will be called at every simulation time step.
 *  @param inputs[] 	Array of values from the input port of your GeckoCIRCUIT Simulation
 *  @param inputLength 	length of inputs[]
 *  @param outputs[] 	Array of values for the output port of your GeckoCIRCUIT Simulation. Fill it with your calculated values
 *  @param outputLength length of outputs[]
 *  @param time 		current simulation time
 *  @param deltaT		simulation time step
 */
void calcOutputs (double inputs[], int inputLength, double outputs[], int outputLength, double time, double deltaT) {
	// your code here
	int i, j;
	double tmpOut = 0;
	if (inputLength > 0 && outputLength > 0) {
		for (i = 0; i < inputLength; i++) {
			tmpOut = tmpOut + inputs[i];
			for (j = i; j < outputLength; j++) {
				outputs[j] = tmpOut;
			}
		}

	 }

}

/**
 *  This function needs to be implemented according to your needs. It will be called once at the beginning (t=0) of your simulation.
 */
void initParameters (void) {
	// your code here ...
}
