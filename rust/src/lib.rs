use bitflags::bitflags;
use brlapi_server::{LouisRequest, ServerBackend, start};
use brlapi_types::keycode::{BrailleCommand, Keycode, KeycodeFlags};
use jni::JNIEnv;
use jni::objects::{JObject, JString, JValue};
use jni::sys::{jbyte, jint, jshort};
use ndarray::Array2;
use tokio::runtime;
use tokio::sync::{mpsc, oneshot};
use xkeysym::Keysym;

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
    let (louis_tx, louis_rx) = mpsc::channel(32);
    {
      let mut env = java_vm.get_env().unwrap();
      let keycode_tx = Box::into_raw(Box::new(keycode_tx)).addr();
      env.set_field(&brlapi_server_object, "keycodeTx", "J", JValue::Long(keycode_tx as i64)).unwrap();
      let louis_tx = Box::into_raw(Box::new(louis_tx)).addr();
      env.set_field(&brlapi_server_object, "louisTx", "J", JValue::Long(louis_tx as i64)).unwrap();
    }
    let backend = ServerBackend { driver_name: "Monarch".to_owned(), model_id: "monarch".to_owned(), columns: 32, lines: 10, braille_tx, keycode_rx, louis_rx };
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
#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_emassey0135_monarchBrlapiServer_BrlapiServer_sendKeys<'local>(
  env: JNIEnv<'local>,
  object: JObject<'local>,
  keys: jint,
) {
  let brlapi_server_object = env.new_global_ref(object).unwrap();
  let java_vm = env.get_java_vm().unwrap();
  RUNTIME.with(|runtime| runtime.block_on(async move {
    java_vm.attach_current_thread_permanently().unwrap();
    let keycode_tx = {
      let mut env = java_vm.get_env().unwrap();
      let keycode_tx = env.get_field(&brlapi_server_object, "keycodeTx", "J").unwrap().j().unwrap();
      unsafe { Box::from_raw(std::ptr::null_mut::<mpsc::Sender<Keycode>>().with_addr(keycode_tx as usize)) }
    };
    let louis_tx = {
      let mut env = java_vm.get_env().unwrap();
      let louis_tx = env.get_field(&brlapi_server_object, "louisTx", "J").unwrap().j().unwrap();
      unsafe { Box::from_raw(std::ptr::null_mut::<mpsc::Sender<LouisRequest>>().with_addr(louis_tx as usize)) }
    };
    let keycode = match keys {
      64 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::BackSpace), braille_command: None },
      128 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Return), braille_command: None },
      256 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::space), braille_command: None },
      512 => Keycode { flags: KeycodeFlags::empty(), keysym: None, braille_command: Some(BrailleCommand::SeveralLinesUp) },
      513 => Keycode { flags: KeycodeFlags::empty(), keysym: None, braille_command: Some(BrailleCommand::SeveralLinesDown) },
      514 => Keycode { flags: KeycodeFlags::empty(), keysym: None, braille_command: Some(BrailleCommand::NextFullWindow) },
      515 => Keycode { flags: KeycodeFlags::empty(), keysym: None, braille_command: Some(BrailleCommand::PreviousFullWindow) },
      516 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Left), braille_command: None },
      517 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Right), braille_command: None },
      518 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Up), braille_command: None },
      519 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Down), braille_command: None },
      520 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Home), braille_command: None },
      521 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::End), braille_command: None },
      522 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Page_Up), braille_command: None },
      523 => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::Page_Down), braille_command: None },
      dots if dots <= 127 => {
        let dots = dots as u32;
        let braille_character: String = char::from_u32(dots+10240).unwrap().into();
        let (result_tx, result_rx) = oneshot::channel();
        louis_tx.send(LouisRequest { tables: "unicode.dis,en-us-comp8.ctb".to_owned(), text: braille_character, backwards: true, result_tx }).await.unwrap();
        let character = result_rx.await.unwrap();
        let character = character.chars().next().unwrap();
        Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::from_char(character)), braille_command: None }
      },
      _ => Keycode { flags: KeycodeFlags::empty(), keysym: Some(Keysym::space), braille_command: None },
    };
    keycode_tx.send(keycode).await.unwrap();
    std::mem::forget(keycode_tx);
    std::mem::forget(louis_tx);
  }));
}
#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_emassey0135_monarchBrlapiServer_BrlapiServer_routeCursor<'local>(
  env: JNIEnv<'local>,
  object: JObject<'local>,
  x: jbyte,
  y: jbyte,
) {
  let brlapi_server_object = env.new_global_ref(object).unwrap();
  let java_vm = env.get_java_vm().unwrap();
  RUNTIME.with(|runtime| runtime.block_on(async move {
    java_vm.attach_current_thread_permanently().unwrap();
    let keycode_tx = {
      let mut env = java_vm.get_env().unwrap();
      let keycode_tx = env.get_field(&brlapi_server_object, "keycodeTx", "J").unwrap().j().unwrap();
      unsafe { Box::from_raw(std::ptr::null_mut::<mpsc::Sender<Keycode>>().with_addr(keycode_tx as usize)) }
    };
    let x = x as u8;
    let y = y as u8;
    let x = x/3;
    let y = y/4;
    let cell: u16 = (y as u16)*32+(x as u16);
    let keycode = Keycode { flags: KeycodeFlags::empty(), keysym: None, braille_command: Some(BrailleCommand::RouteCursorToCharacter { column: cell as u16 }) };
    keycode_tx.send(keycode).await.unwrap();
    std::mem::forget(keycode_tx);
  }));
}
