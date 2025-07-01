use bitflags::bitflags;
use brlapi_server::{ServerBackend, start};
use jni::JNIEnv;
use jni::objects::{JObject, JString, JValue};
use jni::sys::jshort;
use ndarray::Array2;
use tokio::runtime;
use tokio::sync::mpsc;

bitflags! {
  #[derive(Debug, PartialEq, Eq, Clone)]
  pub struct DotFlags: u8 {
    const Dot1 = 1;
    const Dot2 = 1 << 1;
    const Dot3 = 1 << 2;
    const Dot4 = 1 << 3;
    const Dot5 = 1 << 4;
    const Dot6 = 1 << 5;
    const Dot7 = 1 << 6;
    const Dot8 = 1 << 7;
  }
}
thread_local! {
  static RUNTIME: runtime::Runtime = runtime::Builder::new_multi_thread()
    .worker_threads(1)
    .enable_all()
    .build()
    .unwrap();
}
fn braille_matrix_to_dot_matrix(braille_matrix: &Array2<u8>) -> Array2<u8> {
  let mut dot_matrix = Array2::zeros((40, 96));
  for ((line, column), cell) in braille_matrix.indexed_iter() {
    let row_location = line*4;
    let column_location = column*3;
    let dots = DotFlags::from_bits(*cell).unwrap();
    for dot in dots.iter() {
      let (dot_row, dot_column) = match dot {
        DotFlags::Dot1 => (0, 0),
        DotFlags::Dot2 => (1, 0),
        DotFlags::Dot3 => (2, 0),
        DotFlags::Dot7 => (3, 0),
        DotFlags::Dot4 => (0, 1),
        DotFlags::Dot5 => (1, 1),
        DotFlags::Dot6 => (2, 1),
        DotFlags::Dot8 => (3, 1),
        _ => continue
      };
      *dot_matrix.get_mut((row_location+dot_row, column_location+dot_column)).unwrap() = 1;
    };
  };
  dot_matrix
}
#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_emassey0135_monarchBrlapiServer_BrlapiServer_start<'local>(
  mut env: JNIEnv<'local>,
  object: JObject<'local>,
  port: jshort,
  auth_key: JString<'local>,
) {
  let null = JObject::null();
  let auth_key = if env
    .is_same_object(&auth_key, &null)
    .unwrap()
  {
    None
  } else {
    Some(
      env
        .get_string(&auth_key)
        .unwrap()
        .into(),
    )
  };
  let brlapi_server_object = env.new_global_ref(object).unwrap();
  let java_vm = env.get_java_vm().unwrap();
  RUNTIME.with(|runtime| runtime.spawn(async move {
    java_vm.attach_current_thread_permanently().unwrap();
    let (braille_tx, mut braille_rx) = mpsc::channel(32);
    let (keycode_tx, keycode_rx) = mpsc::channel(32);
    let backend = ServerBackend { driver_name: "Monarch".to_owned(), model_id: "monarch".to_owned(), columns: 32, lines: 10, braille_tx, keycode_rx };
    tokio::spawn(async move {
      start(port as u16, auth_key, backend).await;
    });
    while let Some(braille_matrix) = braille_rx.recv().await {
      let dot_matrix = braille_matrix_to_dot_matrix(&braille_matrix);
      let mut env = java_vm.get_env().unwrap();
      let byte_array_class = env.find_class("[B").unwrap();
      let array = env
        .new_object_array(
          40,
          &byte_array_class,
          JObject::null(),
        )
        .unwrap();
      for (index, row) in dot_matrix.outer_iter().enumerate() {
        let row = row.as_slice().unwrap();
        let row = env
          .byte_array_from_slice(&row)
          .unwrap();
        env
          .set_object_array_element(
            &array,
            index as i32,
            row,
          )
          .unwrap();
      };
      env.call_method(&brlapi_server_object, "displayMatrix", "([[B)V", &[JValue::Object(&array)]).unwrap();
    };
  }));
}
