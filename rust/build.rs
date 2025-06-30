extern crate autotools;
use std::env;

fn main() {
  let dest = autotools::Config::new("libiconv-1.18")
    .disable("dependency-tracking", None)
    .enable_static()
    .disable_shared()
    .config_option("host", Some(&env::var("TARGET").unwrap()))
    .build();
  println!("cargo:rustc-link-search=native={}", dest.join("lib").display());
  println!("cargo:rustc-link-lib=static=iconv");
}
