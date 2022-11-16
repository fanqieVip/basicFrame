package com.frame.basic.base.ipc;

import androidx.annotation.Nullable;

/**
 * @Description:
 * @Author: fanj
 * @CreateDate: 2022/8/2 17:10
 * @Version:
 */
public abstract class CallBlock<T> {
    public abstract void success(@Nullable T data);

    public void error(@Nullable String error) {
    }
}
