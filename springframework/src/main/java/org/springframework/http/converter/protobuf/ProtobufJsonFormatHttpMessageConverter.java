//
//
// package org.springframework.http.converter.protobuf;
//
// import com.google.protobuf.ExtensionRegistry;
// import com.google.protobuf.util.JsonFormat;
//
// import org.springframework.lang.Nullable;
//
// public class ProtobufJsonFormatHttpMessageConverter extends ProtobufHttpMessageConverter {
//
//     public ProtobufJsonFormatHttpMessageConverter() {
//         this(null, null, (ExtensionRegistry) null);
//     }
//
//     public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {
//
//         this(parser, printer, (ExtensionRegistry) null);
//     }
//
//     public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {
//
//         super(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
//     }
//
//     @Deprecated
//     public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistryInitializer registryInitializer) {
//
//         super(new ProtobufJavaUtilSupport(parser, printer), null);
//         if (registryInitializer != null) {
//             registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
//         }
//     }
//
// }
