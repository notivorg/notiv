{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";

    clojure-nix-locker.url = "github:bevuta/clojure-nix-locker";
    clojure-nix-locker.inputs.nixpkgs.follows = "nixpkgs";
    clojure-nix-locker.inputs.flake-utils.follows = "flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, clojure-nix-locker }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        clj-locker = clojure-nix-locker.lib.customLocker {
          inherit pkgs;
          command = "${pkgs.clojure}/bin/clojure -A:cljs:shadow-cljs -Stree";
          lockfile = "deps.lock.json";
          src = ./.;
        };

        npm-cache = pkgs.fetchNpmDeps {
          src = ./.;
          hash = "sha256-P9PG0bM3gt2J43ycMTzoABFqT5MQQL8K4ZNCJwwbNRE=";
        };
      in {
        devShell = pkgs.mkShell {
          buildInputs = with pkgs; [
            (pkgs.clojure.override { jdk = pkgs.jdk17; })
            nodePackages.npm
            nodejs
            jdk17
            babashka
            clj-kondo
          ];
        };

        apps.clj-locker = flake-utils.lib.mkApp { drv = clj-locker.locker; };

        packages.default = pkgs.stdenv.mkDerivation {
          pname = "notive";
          version = "alpha";
          src = ./.;

          nativeBuildInputs = with pkgs; [
            coreutils
            clojure
            babashka
            nodejs
            breakpointHook
          ];

          npm_config_cache = npm-cache;

          buildPhase = ''
            source ${clj-locker.shellEnv}
            npm install
            bb release
          '';

          installPhase = ''
            mkdir -p $out
            mv target/release/* $out
          '';
        };
      });
}
