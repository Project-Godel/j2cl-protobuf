package com.google.protobuf.contrib.immutablejs.generator;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Maps;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protos.proto2.compiler.Plugin.CodeGeneratorRequest;
import com.google.protos.proto2.compiler.Plugin.CodeGeneratorResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Protobuf compiler plugin that generates the Immutable JavaScript protobuf implementation. */
public class ImmutableJspbCompiler {

  public static void main(String[] args) throws IOException, DescriptorValidationException {
    CodeGeneratorRequest request =
        CodeGeneratorRequest.parseFrom(System.in, ExtensionRegistry.getGeneratedRegistry());


    Map<String, FileDescriptor> fileDescriptors = Maps.newHashMap();
    CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();
    CodeWriter codeWriter = new CodeWriter(response);

    List<String> fileToGenerateList = request.getFileToGenerateList();

    for (FileDescriptorProto fileDescriptorProto : request.getProtoFileList()) {
      // Look up the imported files from previous file descriptors.  It is sufficient to look at
      // only previous file descriptors because CodeGeneratorRequest guarantees that the files
      // are sorted in topological order.
      FileDescriptor[] deps = new FileDescriptor[fileDescriptorProto.getDependencyCount()];
      for (int i = 0; i < fileDescriptorProto.getDependencyCount(); i++) {
        String name = fileDescriptorProto.getDependency(i);
        FileDescriptor dependee = checkNotNull(fileDescriptors.get(name),
            "Missing file descriptor for [%s]", name);
        deps[i] = dependee;
      }

      // Build and cache the current file descriptor.
      FileDescriptor fileDescriptor = FileDescriptor.buildFrom(fileDescriptorProto, deps);
      fileDescriptors.put(fileDescriptor.getName(), fileDescriptor);

      // We only generate output for protos that we see on the command line not their
      // dependencies. This is necessary to make strict deps work.
      if (!fileToGenerateList.contains(fileDescriptorProto.getName())) {
        continue;
      }

      // Generate code based on the current file descriptor.
      new TemplateRenderer(codeWriter, fileDescriptor).generateCode();
    }
    response.build().writeTo(System.out);
  }
}