DO
$$
BEGIN
create type ${schema}.http_method_type as enum ('GET','HEAD','POST','PUT','DELETE','CONNECT','OPTIONS','TRACE','PATCH');
EXCEPTION
            WHEN duplicate_object THEN null;
END
$$;

