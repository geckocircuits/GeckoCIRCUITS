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

/**
 * Do not edit any function names! JNI expects them to know that way!
 */
#ifndef TEST_JNI_DLL_H_
#define TEST_JNI_DLL_H_

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     gecko_geckocircuits_nativec_NativeCWrapper
 * Method:    calcOutputs
 * Signature: ([D[DIDD)V
 */
JNIEXPORT void JNICALL Java_gecko_geckocircuits_nativec_NativeCWrapper_calcOutputs
  (JNIEnv *, jobject, jdoubleArray, jdoubleArray, jint, jdouble, jdouble);

/*
 * Class:     gecko_geckocircuits_nativec_NativeCWrapper
 * Method:    initParameters
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gecko_geckocircuits_nativec_NativeCWrapper_initParameters
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif

void calcOutputs (double inputs[], int inputLength, double outputs[], int outputLength, double time, double deltaT);

void initParameters (void);

#endif /* TEMPLATE_JNI_LIB_H_ */
