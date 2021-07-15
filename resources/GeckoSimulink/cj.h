#ifndef __CJ__
#define __CJ__

#include "jni.h"


/**
 * CJ is a C API that works in conjuction with a Java package to
 * simplify the C-to-Java interface of the JNI.  On the C side, CJ
 * wraps the invocation API's creation and destruction of the JVM.,
 * and hides the complexity of JNI calls, JNI garbage collection
 * and string and array handling.
 */

/* Constants */
#define CJ_STRMAX 1000
#define CJ_MAX_METHODS 20

/* Error codes */
#define CJ_ERR_SUCCESS 0
#define CJ_ERR_INPUT 1
#define CJ_ERR_MEM 2
#define CJ_ERR_JVM_CONNECT 100
#define CJ_ERR_JVM_DISCONNECT 101
#define CJ_ERR_JNI 102
#define CJ_ERR_DATA 200
#define CJ_ERR_OTHER 9000


/**
 * CJ is a C API that works in conjuction with a Java package to
 * simplify the C-to-Java interface of the JNI.  On the C side, CJ
 * wraps the invocation API's creation and destruction of the JVM.,
 * and hides the complexity of JNI calls, JNI garbage collection
 * and string and array handling.
 */

#define CJ_STRMAX 1000
#define CJ_MAX_METHODS 20

#define CJ_ERR_SUCCESS 0
#define CJ_ERR_INPUT 1
#define CJ_ERR_MEM 2
#define CJ_ERR_JVM_CONNECT 100
#define CJ_ERR_JVM_DISCONNECT 101
#define CJ_ERR_JNI 102
#define CJ_ERR_DATA 200
#define CJ_ERR_OTHER 9000

#define CJ_TYPE_INT 1
#define CJ_TYPE_BOOL 2
#define CJ_TYPE_STRING 3

/* Represents a JVM */
typedef struct
{
   JavaVM *jvm;
   JNIEnv *jni;
} cjJVM_t;

/* Represents a method */
typedef struct
{
   char *methodName;
   char *methodSig;
   jmethodID method;
} cjMethod_t;

/* Represents a class, including its methods */
typedef struct
{
   cjJVM_t *jvm;
   char *className;
   jclass clazz;
   int numMethods;
   cjMethod_t *methods;
   jboolean ok;
} cjClass_t;

/* Represents an object, including its class */
typedef struct
{
   cjClass_t *clazz;
   jobject object;
   jboolean ok;
} cjObject_t;

/* JVM */
extern int cjJVMConnect(cjJVM_t *pJVM);
extern int cjJVMDisconnect(cjJVM_t *pJVM);

/* Create, destroy classes */
extern int cjClassCreate(cjClass_t *pClass);
extern int cjClassDestroy(cjClass_t *pClass);

/* Proxy */
extern int cjProxyClassCreate(cjClass_t *pClass, char *className, 
   cjJVM_t *pJVM);
extern int cjProxyCreate(cjObject_t *pProxy, char * inData);

extern int cjFreeObject(cjJVM_t *pJVM, jobject object);

/* Proxy Test */
extern void cjProxyExecOpenFile(cjObject_t *pProxy, char *inData, char* output);
extern void cjProxyExecGeckoInit(cjObject_t *pProxy, double tFinal);
extern void cjProxyEndGecko(cjObject_t *pProxy);
extern int acquireGlobalReference(cjJVM_t *pJVM, jobject loObj, jobject *pGloObj);
extern jboolean checkException(cjJVM_t *pJVM);
#endif
