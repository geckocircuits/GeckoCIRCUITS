/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */

#define S_FUNCTION_LEVEL 2
#define S_FUNCTION_NAME  s_GeckoCIRCUITS


/* define error messages */
#define ERR_INVALID_SET_INPUT_DTYPE_CALL  \
              "Invalid call to mdlSetInputPortDataType"

#define ERR_INVALID_SET_OUTPUT_DTYPE_CALL \
              "Invalid call to mdlSetOutputPortDataType"

#define ERR_INVALID_DTYPE     "Invalid input or output port data type"


/*
 * Need to include simstruc.h for the definition of the SimStruct and
 * its associated macro definitions.
 */
#include "simstruc.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "jni.h"

#include "cj.h"

static cjObject_t proxy;
static cjJVM_t jvm;
static cjClass_t proxyClass;
static   int rc;
static   char sout[1000];
static initCounter = 0;
static double oldtime = 0;

static cjMethod_t proxyMethods[] =
{
   {"<init>", "(Ljava/lang/String;)V", NULL},
   /*{"<init>", "(Ljava/lang/Object;)V", NULL},
   //{"external_openFile", "(Ljava/lang/Object;)V", NULL},*/
   {"external_openFile", "(Ljava/lang/Object;)Ljava/lang/Object;", NULL},
   {"external_init", "(D)D", NULL},
   {"external_step", "(D)V", NULL},
   {"external_end", "()V", NULL},
   {"external_getTerminalNumber_TO_EXTERNAL", "(I)I", NULL},
   {"external_getTerminalNumber_FROM_EXTERNAL", "(I)I", NULL},
   {"external_getdt", "()D", NULL},
   {"external_getValues", "(I)[D", NULL},
   {"getNumOutputPorts", "()I", NULL},
   {"external_setScalarInputValue", "(DI)V", NULL},
   {"getNumInputPorts", "()I", NULL},
   /*{"external_setPortName", "(ILjava/lang/String;)V", NULL },*/
   {"external_setVectorInputValue", "(DII)V", NULL},
};


int getOutTerminalNumber(int portNr) {
    int value;
     cjClass_t *pClass = proxy.clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   
   value = (*env)->CallIntMethod(env, proxy.object, 
        (pClass->methods[5]).method, portNr);
    return value;
}

int getOutPortNumber() {
    int value;
     cjClass_t *pClass = proxy.clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   
   value = (*env)->CallIntMethod(env, proxy.object, 
        (pClass->methods[9]).method); 
    return value;
}

int getInPortNumber() {
   int value;
     cjClass_t *pClass = proxy.clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   
   value = (*env)->CallIntMethod(env, proxy.object, 
        (pClass->methods[11]).method);
   
    return value;
}


int getInTerminalNumber(int portNo) {
    int value;
     cjClass_t *pClass = proxy.clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   
   value = (*env)->CallIntMethod(env, proxy.object, 
        (pClass->methods[6]).method, portNo); 
    return value;
}



double getDt() {
    double value;
     cjClass_t *pClass = proxy.clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   
   value = (*env)->CallDoubleMethod(env, proxy.object, 
        (pClass->methods[7]).method); 
    return value;
}


void setName(int index, const char* name) {
    double value;
    jstring instring = NULL;
   
     cjClass_t *pClass = proxy.clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   
   instring = (*env)->NewStringUTF(env, name);
     
   (*env)->CallObjectMethod(env, proxy.object, 
        (pClass->methods[12]).method, index, instring); 

}

static void mdlInitializeSizes(SimStruct *S)
{
    
    char *Port_name ;
    int_T buflen;
    int_T status;
    int outTerminalNumber = 1;
    int outPortNumber = 1;
    int inPortNumber = 1;
    int inTerminalNumber = 1;
    int i, k;
    char filename[256];
    const char* portName;
   
    ssSetNumSFcnParams(S, ssGetSFcnParamsCount(S));  /* Number of expected parameters */
    /*if (ssGetNumSFcnParams(S) != ssGetSFcnParamsCount(S)) {
        Return if number of expected != number of actual parameters 
        return;
    } */

    mxGetString(ssGetSFcnParam(S, 0), filename, 256);
    
    if( initCounter == 0) {
       initCounter++;
    memset(&sout, 0, 1000);
    
    memset(&jvm, 0, sizeof(cjJVM_t));
    memset(&proxyClass, 0, sizeof(cjClass_t));
    memset(&proxy, 0, sizeof(cjObject_t));
    
    rc = cjJVMConnect(&jvm);
   
    
    /*mexEvalString("port_label('output', 1, 'test')");*/
    
   rc = cjProxyClassCreate(&proxyClass, "gecko/GeckoSimulink", &jvm);
   proxy.clazz = &proxyClass;
   rc = cjProxyCreate(&proxy, filename);
     
   }
   
    cjProxyExecGeckoInit(&proxy, S->mdlInfo->tFinal);
    
    
    ssSetNumContStates(S, 0);
    ssSetNumDiscStates(S, 0);
    inPortNumber = getInPortNumber();
    
    if (!ssSetNumInputPorts(S, inPortNumber)) return;
    
    if(inPortNumber < 1) {
        mexWarnMsgTxt("No input port ('from External' found in Gecko model.");
    }
    
   
    for(i = 0; i < inPortNumber; i++) {
        ssSetInputPortWidth(S,i, getInTerminalNumber(i) );
        ssSetInputPortDataType(S, i, DYNAMICALLY_TYPED);    
    }
    
    if(inPortNumber > 0) 
    ssSetInputPortDirectFeedThrough(S, 0, 0);
    
    outPortNumber = getOutPortNumber();
    
    
    if (!ssSetNumOutputPorts(S, outPortNumber)) return;
    
    for(i = 0; i < outPortNumber; i++) {
        ssSetOutputPortWidth(S, i, getOutTerminalNumber(i) );
    }
    

    ssSetNumSampleTimes(S, 1);
    ssSetNumRWork(S, 0);
    ssSetNumIWork(S, 0);
    ssSetNumPWork(S, 0);
    ssSetNumModes(S, 0);
    ssSetNumNonsampledZCs(S, 0);
    
    ssSetOptions(S, 0);   

    /*buflen = mxGetN((ssGetSFcnParam(S, 0)))*sizeof(mxChar)+1;
    Port_name = mxMalloc(buflen);
    status = mxGetString((ssGetSFcnParam(S, 1)),Port_name,buflen);
    mexPrintf("The Input Port Name is - %s\n ", Port_name);*/
    
}

/* Function: isAcceptableDataType
 *    determine if the data type ID corresponds to an unsigned integer
 */
static boolean_T isAcceptableDataType(DTypeId dataType) 
{
       
    boolean_T isAcceptable = (dataType == SS_BOOLEAN  || 
                              dataType == SS_DOUBLE);
    
    return isAcceptable;
}


#define MDL_SET_INPUT_PORT_DATA_TYPE
/* Function: mdlSetInputPortDataType ==========================================
 *    This routine is called with the candidate data type for a dynamically
 *    typed port.  If the proposed data type is acceptable, the routine should
 *    go ahead and set the actual port data type using ssSetInputPortDataType.
 *    If the data type is unacceptable an error should generated via
 *    ssSetErrorStatus.  Note that any other dynamically typed input or
 *    output ports whose data types are implicitly defined by virtue of knowing
 *    the data type of the given port can also have their data types set via 
 *    calls to ssSetInputPortDataType or ssSetOutputPortDataType.
 */
static void mdlSetInputPortDataType(SimStruct *S, 
                                    int       port, 
                                    DTypeId   dataType)
{
        if( isAcceptableDataType( dataType ) ) {
            /*
             Accept proposed data type
             force all data ports to use this data type.
	*/            
            ssSetInputPortDataType(  S, port, dataType );                     
        } else {
            /* Reject proposed data type */
            ssSetErrorStatus(S,ERR_INVALID_DTYPE);
        }
   
    return;
}

/* mdlSetInputPortDataType */

/* Function: mdlInitializeSampleTimes =========================================
 * Abstract:
 *    This function is used to specify the sample time(s) for your
 *    S-function. You must register the same number of sample times as
 *    specified in ssSetNumSampleTimes.
 */
static void mdlInitializeSampleTimes(SimStruct *S)
{    
    ssSetSampleTime(S, 0, getDt());
    ssSetOffsetTime(S, 0, 0.0);
}



#define MDL_INITIALIZE_CONDITIONS   /* Change to #undef to remove function */
#if defined(MDL_INITIALIZE_CONDITIONS)
  /* Function: mdlInitializeConditions ========================================
   * Abstract:
   *    In this function, you should initialize the continuous and discrete
   *    states for your S-function block.  The initial states are placed
   *    in the state vector, ssGetContStates(S) or ssGetRealDiscStates(S).
   *    You can also perform any other initialization activities that your
   *    S-function may require. Note, this routine will be called at the
   *    start of simulation and if it is present in an enabled subsystem
   *    configured to reset states, it will be call when the enabled subsystem
   *    restarts execution to reset the states.
   */
  static void mdlInitializeConditions(SimStruct *S)
  {
  }
#endif /* MDL_INITIALIZE_CONDITIONS */



#define MDL_START  /* Change to #undef to remove function */
#if defined(MDL_START) 
  /* Function: mdlStart =======================================================
   * Abstract:
   *    This function is called once at start of model execution. If you
   *    have states that should be initialized once, this is the place
   *    to do it.
   */
  static void mdlStart(SimStruct *S)
  {
  }
#endif /*  MDL_START */




/* Function: mdlOutputs =======================================================
 * Abstract:
 *    In this function, you compute the outputs of your S-function
 *    block. Generally outputs are placed in the output vector, ssGetY(S).
 */
static void mdlOutputs(SimStruct *S, int_T tid)
{
    
}



#define MDL_UPDATE  /* Change to #undef to remove function */
#if defined(MDL_UPDATE)
  /* Function: mdlUpdate ======================================================
   * Abstract:
   *    This function is called once for every major integration time step.
   *    Discrete states are typically updated here, but this function is useful
   *    for performing any tasks that should only take place once per
   *    integration step.
   */
  static void mdlUpdate(SimStruct *S, int_T tid)
  {  
    
    double* output;
    double test; 
    int_T i,j, k, l;
    
   time_T time; 
   cjClass_t *pClass = proxy.clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   jobject loRef = NULL;
   jdoubleArray tmpRef;
   double x[50];
   int_T width;
   int_T inWidth;
   int_T nInputPorts = ssGetNumInputPorts(S);
   DTypeId   y0DataType;
   real_T *y;
    
   for(k = 0; k < nInputPorts; k++) {
        inWidth = ssGetInputPortWidth(S, k);
        y0DataType = ssGetInputPortDataType(S, k);
    
        if(y0DataType == SS_BOOLEAN) {
            InputBooleanPtrsType pU0  = (InputBooleanPtrsType) ssGetInputPortSignalPtrs(S,k);     
            for( i = 0; i < inWidth; ++i){
                x[i] =(double)*pU0[i];
            }
        }
     
        if(y0DataType == SS_DOUBLE) {
            InputRealPtrsType uPtrs  = ssGetInputPortRealSignalPtrs(S,k);
            for (i = 0; i < inWidth; i++) {
                x[i] =  *uPtrs[i];
            }
        }
        
        if(inWidth == 1) {
            (*env)->CallObjectMethod(env, proxy.object, 
                (pClass->methods[10]).method, x[0], k);
        } else {
            for(l = 0; l < inWidth; l++) {
                (*env)->CallObjectMethod(env, proxy.object, 
                    (pClass->methods[12]).method, x[l], k, l);
            }
        }
        
        
    }
    time = ssGetT(S);
    /*if(time <= oldtime) {
        printf("time error*\n\n");
    }*/
    oldtime = time;
    
    (*env)->CallObjectMethod(env, proxy.object, 
         (pClass->methods[3]).method, time);
      
      for(i = 0; i < ssGetNumOutputPorts(S); i++) {
        
         width = ssGetOutputPortWidth(S,i);
         
         tmpRef = (jdoubleArray) (*env)->CallObjectMethod(env, proxy.object, 
         (pClass->methods[8]).method, i);
          
         output = (*env)->GetDoubleArrayElements(env, tmpRef, 0);
         y = ssGetOutputPortRealSignal(S,i);
         
        for (j=0; j < width; j++) {
             *y++ = output[j];
        }
      
        (*env)->ReleaseDoubleArrayElements(env, tmpRef, output, 0);
      }
      }
#endif /* MDL_UPDATE */



#define MDL_DERIVATIVES  /* Change to #undef to remove function */
#if defined(MDL_DERIVATIVES)
  /* Function: mdlDerivatives =================================================
   * Abstract:
   *    In this function, you compute the S-function block's derivatives.
   *    The derivatives are placed in the derivative vector, ssGetdX(S).
   */
  static void mdlDerivatives(SimStruct *S)
  {
  }
#endif /* MDL_DERIVATIVES */



/* Function: mdlTerminate =====================================================
 * Abstract:
 *    In this function, you should perform any actions that are necessary
 *    at the termination of a simulation.  For example, if memory was
 *    allocated in mdlInitializeConditions, this is the place to free it.
 */
static void mdlTerminate(SimStruct *S)
{
    cjProxyEndGecko(&proxy);
}




/*
 * Load given class and get its methods
 */
int cjClassCreate(cjClass_t *pClass)
{
   long jret = 0;
   int rc = CJ_ERR_SUCCESS;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   jboolean isException = JNI_FALSE;
   int i = 0;

   pClass->ok = JNI_FALSE;

   /* Get class */
   if (rc == CJ_ERR_SUCCESS)
   {
      pClass->clazz = (*env)->FindClass(env, pClass->className);
      isException = checkException(jvm);
      if (isException || pClass->clazz == NULL)
      {
         rc = CJ_ERR_JNI;
      }
   }   


   for (i = 0; i < pClass->numMethods; i++)
   {
      if (rc == CJ_ERR_SUCCESS)
      {
         cjMethod_t *pMethod = &((pClass->methods)[i]);
         pMethod->method = (*env)->GetMethodID(env, pClass->clazz, 
            pMethod->methodName, pMethod->methodSig);
         isException = checkException(jvm);
         if (isException || pMethod->method == NULL)
         {
            rc = CJ_ERR_JNI;
         }
      }
   }

   if (rc == CJ_ERR_SUCCESS)
   {
      pClass->ok = JNI_TRUE;
   }

   return rc;
}

/*
 * Instantiate a proxy
 */
int cjProxyCreate(cjObject_t *pProxy, char *inData) {
   long jret = 0;
   int rc = CJ_ERR_SUCCESS;
   jstring instring = NULL;
   
   cjClass_t *pClass = pProxy->clazz;
   cjJVM_t *pJVM = pClass->jvm;
   JNIEnv *env = pJVM->jni;
   jboolean isException = JNI_FALSE;
   jobject loObj = NULL;

   pProxy->ok = JNI_FALSE;

   
   
   /* Create string */
      instring = (*env)->NewStringUTF(env, inData);
   
   
   
   /* Get local reference, from this get global reference */
   if (rc == CJ_ERR_SUCCESS)
   {
      loObj = (*env)->NewObject(env, pClass->clazz, 
         ((pClass->methods)[0]).method, instring);
      isException = checkException(pJVM);
      if (isException)
      {
         rc = CJ_ERR_JNI;
      }

      rc = acquireGlobalReference(pJVM, loObj, &(pProxy->object));
      isException = checkException(pJVM);
      if (isException)
      {
         rc = CJ_ERR_JNI;
      }
   }

   /* if we failed, cleanup */
   if (rc == CJ_ERR_SUCCESS)
   {
      pProxy->ok = JNI_TRUE;
   }
   return rc;
}


/**
 * Creates a proxy class with the given class name.
 * The methods are given by the proxy spec above.
 */
int cjProxyClassCreate(cjClass_t *pClass, char *className, 
   cjJVM_t *pJVM)
{
   pClass->className = className;
   pClass->jvm = pJVM;
   pClass->numMethods = 13; /* nummeth */
   pClass->methods = proxyMethods;
   return cjClassCreate(pClass);
}


/*
 * Connect to JVM using arguments in pJVM.  If failure, clean up.
 * Returns CJ_ERR_SUCCESS if sucessful.
 */
int cjJVMConnect(cjJVM_t *pJVM)
{
   int rc = CJ_ERR_SUCCESS;

   int ret = 0;
   JavaVM** vmBuf;
   
   jsize buflen = 1;
   jsize nVM[1]; 
   
   
   vmBuf = (JavaVM **) malloc(sizeof(JavaVM *) * 10);
   
    ret = JNI_GetCreatedJavaVMs(vmBuf, buflen, nVM);
   
   /* create VM */
   if (rc == CJ_ERR_SUCCESS)
   {
      JavaVM *jvm = vmBuf[0];
      JNIEnv *jni;

     (*jvm)->AttachCurrentThread(jvm, (void **)&jni, NULL);

         pJVM->jvm = jvm;
         pJVM->jni = jni;
   }

   /* if we failed, cleanup */
   if (rc != CJ_ERR_SUCCESS)
   {
      cjJVMDisconnect(pJVM);
   }
   
   return rc; 
}

void cjProxyExecGeckoInit(cjObject_t *pProxy, double tFinal)
{    
   long jret = 0;
   cjClass_t *pClass = pProxy->clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;
   jboolean isException = JNI_FALSE;
   jobject loRef = NULL;    
      (*env)->CallObjectMethod(env, pProxy->object, 
         (pClass->methods[2]).method, tFinal);     
}



void cjProxyEndGecko(cjObject_t *pProxy)
{
  long jret = 0;

   cjClass_t *pClass = pProxy->clazz;
   cjJVM_t *jvm = pClass->jvm;
   JNIEnv *env = jvm->jni;

   (*env)->CallObjectMethod(env, pProxy->object, 
      (pClass->methods[4]).method);

}

/**
 * Acquire global reference (pGloObj) based on local reference (loObj).
 * Release the local reference.
 */
int acquireGlobalReference(cjJVM_t *pJVM, jobject loObj, jobject *pGloObj)
{
   long jret = 0;
   int rc = CJ_ERR_SUCCESS;
   JNIEnv *env = pJVM->jni;
   jboolean isException = JNI_FALSE;

   isException = checkException(pJVM);
   if (isException || loObj == NULL)
   {
      rc = CJ_ERR_JNI;
   }

   /* create global reference */
   if (rc == CJ_ERR_SUCCESS)
   {
      *pGloObj = (*env)->NewGlobalRef(env, loObj);
      isException = checkException(pJVM);
      if (isException || *pGloObj == NULL)
      {
         rc = CJ_ERR_JNI;
      }
   }   

   /* delete local reference */
   if (loObj != NULL)
   {
      (*env)->DeleteLocalRef(env, loObj);
      isException = checkException(pJVM);
      if (isException)
      {
         rc = CJ_ERR_JNI;
      }
   }

   return rc;
}

/**
 * Convert jstring to c zero-terminated string
 */
int jstring2cstring(cjJVM_t *pJVM, jstring js, char *cs)
{
   long jret = 0;
   int rc = CJ_ERR_SUCCESS;
   JNIEnv *env = pJVM->jni;
   jboolean isException = JNI_FALSE;

   /* copy object into a buffer */
   if (rc == CJ_ERR_SUCCESS)
   {
      const char *tempData;

      tempData = (*env)->GetStringUTFChars(env, js, 0);
      if (tempData == NULL)
      {
         rc = CJ_ERR_MEM;
      }

      isException = checkException(pJVM);
      if (isException)
      {
         rc = CJ_ERR_JNI;
      }

      if (rc == CJ_ERR_SUCCESS)
      {
         /* copy to caller's buffer and release the UTF */
         strcpy(cs, (char*)tempData);
         (*env)->ReleaseStringUTFChars(env, js, tempData); 
         isException = checkException(pJVM);
         if (isException)
         {
            rc = CJ_ERR_JNI;
         }
      }
   }

   return rc;
}

/**
 * Check for exception and clear it.  Return CJ_ERR_SUCCESS if no exception
 */
jboolean checkException(cjJVM_t *pJVM)
{
   JNIEnv *env = pJVM->jni;
   jboolean isException = (*env)->ExceptionCheck(env);
   if (isException)
   {
      (*env)->ExceptionDescribe(env); /* capture this somewhere */
      (*env)->ExceptionClear(env);
   }
   return isException;
}



/*======================================================*
 * See sfuntmpl.doc for the optional S-function methods *
 *======================================================*/

/*=============================*
 * Required S-function trailer *
 *=============================*/

#ifdef  MATLAB_MEX_FILE    /* Is this file being compiled as a MEX-file? */
#include "simulink.c"      /* MEX-file interface mechanism */
#else
#include "cg_sfun.h"       /* Code generation registration function */
#endif
