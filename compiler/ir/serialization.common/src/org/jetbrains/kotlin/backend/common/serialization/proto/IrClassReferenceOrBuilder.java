// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

public interface IrClassReferenceOrBuilder extends
    // @@protoc_insertion_point(interface_extends:org.jetbrains.kotlin.backend.common.serialization.proto.IrClassReference)
    org.jetbrains.kotlin.protobuf.MessageLiteOrBuilder {

  /**
   * <code>required int32 class_symbol = 1;</code>
   */
  boolean hasClassSymbol();
  /**
   * <code>required int32 class_symbol = 1;</code>
   */
  int getClassSymbol();

  /**
   * <code>required int32 class_type = 2;</code>
   */
  boolean hasClassType();
  /**
   * <code>required int32 class_type = 2;</code>
   */
  int getClassType();
}