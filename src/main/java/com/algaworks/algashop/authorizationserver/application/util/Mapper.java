package com.algaworks.algashop.authorizationserver.application.util;

// conversor generico, usado para passar dto para domain por exemplo
public interface Mapper {
    <T> T convert(Object object, Class<T> destinationType);
}
