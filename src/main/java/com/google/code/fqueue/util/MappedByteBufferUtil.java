///*
// *  Copyright 2011 sunli [sunli1223@gmail.com][weibo.com@sunli1223]
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// */
//
//package com.google.code.fqueue.util;
//
//
//import java.lang.ref.Cleaner;
//import java.lang.reflect.Method;
//import java.nio.MappedByteBuffer;
//import java.security.AccessController;
//import java.security.PrivilegedAction;
//
///**
// * @author sunli
// */
//public class MappedByteBufferUtil {
//
//    public static void clean(final Object buffer) {
//        AccessController.doPrivileged(new PrivilegedAction() {
//            public Object run() {
//                try {
//
//                    Method getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
//                    getCleanerMethod.setAccessible(true);
//                    Cleaner cleaner = (Cleaner) getCleanerMethod.invoke(buffer, new Object[0]);
//                    cleaner.clean();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//        });
//
//
//    }
//
//    @Override
//    public void run() {
//
//    }
//
//    public void invokeCleaner(java.nio.ByteBuffer directBuffer) {
//        if (!directBuffer.isDirect())
//            throw new IllegalArgumentException("buffer is non-direct");
//
//        DirectBuffer db = (DirectBuffer) directBuffer;
//        if (db.attachment() != null)
//            throw new IllegalArgumentException("duplicate or slice");
//
//        Cleaner cleaner = db.cleaner();
//        if (cleaner != null) {
//            cleaner.clean();
//        }
//    }
//
//    public static void main(String[] args) {
//
//        try {
//
//            Method getCleanerMethod = MappedByteBuffer.class.getMethod("cleaner", new Class[0]);
//            System.out.println(getCleanerMethod);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//}
