@echo off
rem
rem Copyright (c) Microsoft. All rights reserved.
rem Licensed under the MIT license. See License.txt in the repository root.
rem 
rem Wraps build-inner.cmd, which changes the environment, so the outer environment isn't changed.

cmd /c build-inner.cmd %*